package com.example.ecommerce.order.service;

import com.example.ecommerce.order.client.InventoryClient;
import com.example.ecommerce.order.dto.request.OrderRequestItem;
import com.example.ecommerce.order.dto.request.PlaceOrderRequest;
import com.example.ecommerce.order.dto.response.OrderHistoryResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusResponse;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderItem;
import com.example.ecommerce.order.entity.OrderStatus;
import com.example.ecommerce.order.entity.OrderStatusHistory;
import com.example.ecommerce.order.entity.ProcessedEvent;
import com.example.ecommerce.order.event.DomainEvent;
import com.example.ecommerce.order.event.EventTypes;
import com.example.ecommerce.order.event.payload.InventoryReservationFailedPayload;
import com.example.ecommerce.order.event.payload.OrderShippedPayload;
import com.example.ecommerce.order.event.payload.PaymentFailedPayload;
import com.example.ecommerce.order.exception.OrderNotFoundException;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.outbox.OutboxEventService;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.order.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final OrderStatusHistoryRepository history;
    private final InventoryClient inventoryClient;
    private final OrderMapper mapper;
    private final OutboxEventService outboxEventService;
    private final ProcessedEventRepository processedEvents;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Autowired
    public OrderService(
            OrderRepository orders,
            OrderItemRepository items,
            OrderStatusHistoryRepository history,
            InventoryClient inventoryClient,
            OrderMapper mapper,
            OutboxEventService outboxEventService,
            ProcessedEventRepository processedEvents,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.orders = orders;
        this.items = items;
        this.history = history;
        this.inventoryClient = inventoryClient;
        this.mapper = mapper;
        this.outboxEventService = outboxEventService;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    protected OrderService(
            OrderRepository orders,
            OrderItemRepository items,
            OrderStatusHistoryRepository history,
            InventoryClient inventoryClient,
            OrderMapper mapper
    ) {
        this(orders, items, history, inventoryClient, mapper, null, null, null, null);
    }

    @Transactional
    public OrderResponse place(PlaceOrderRequest request) {
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Instant now = Instant.now();
        BigDecimal totalAmount = total(request.items());
        Order order = orders.save(new Order(orderId, request.customerId(), totalAmount, OrderStatus.CREATED, now, now));
        List<OrderItem> orderItems = request.items().stream()
                .map(item -> toEntity(orderId, item))
                .toList();
        items.saveAll(orderItems);
        history.save(new OrderStatusHistory(
                UUID.randomUUID(),
                orderId,
                OrderStatus.CREATED,
                "Order created and awaiting asynchronous processing",
                now
        ));
        outboxEventService.saveOrderPlaced(order, orderItems, correlationId);
        meterRegistry.counter("orders.created.count").increment();
        log.info(
                "correlationId={} orderId={} service=order-service eventType={} message=\"Order created with outbox event\"",
                correlationId,
                orderId,
                EventTypes.ORDER_PLACED
        );
        return mapper.toResponse(order, orderItems);
    }

    public OrderResponse get(UUID orderId) {
        Order order = require(orderId);
        return mapper.toResponse(order, items.findByOrderId(orderId));
    }

    public OrderStatusResponse status(UUID orderId) {
        Order order = require(orderId);
        return new OrderStatusResponse(order.getId(), order.getStatus().name());
    }

    public List<OrderResponse> customerOrders(UUID customerId) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(order -> mapper.toResponse(order, items.findByOrderId(order.getId())))
                .toList();
    }

    public List<OrderHistoryResponse> history(UUID orderId) {
        require(orderId);
        return history.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void applyWorkflowEvent(DomainEvent event) {
        if (processedEvents.existsById(event.eventId())) {
            log.info(
                    "correlationId={} eventId={} orderId={} service=order-service eventType={} message=\"Duplicate event skipped\"",
                    event.correlationId(),
                    event.eventId(),
                    event.aggregateId(),
                    event.eventType()
            );
            return;
        }

        OrderStatus status = statusFor(event);
        Order order = require(event.aggregateId());
        Instant now = Instant.now();
        order.updateStatus(status, now);
        history.save(new OrderStatusHistory(UUID.randomUUID(), order.getId(), status, notesFor(event), now));
        processedEvents.save(new ProcessedEvent(event.eventId(), event.eventType(), event.aggregateId(), now));
        if (status == OrderStatus.CANCELLED) {
            meterRegistry.counter("orders.cancelled.count").increment();
        }
        if (status == OrderStatus.SHIPPED || status == OrderStatus.CONFIRMED) {
            meterRegistry.counter("orders.confirmed.count").increment();
        }
    }

    private Order require(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderStatus statusFor(DomainEvent event) {
        return switch (event.eventType()) {
            case EventTypes.INVENTORY_RESERVED -> OrderStatus.INVENTORY_RESERVED;
            case EventTypes.INVENTORY_RESERVATION_FAILED -> OrderStatus.CANCELLED;
            case EventTypes.PAYMENT_PROCESSED -> OrderStatus.PAYMENT_PROCESSED;
            case EventTypes.PAYMENT_FAILED -> OrderStatus.CANCELLED;
            case EventTypes.ORDER_SHIPPED -> OrderStatus.SHIPPED;
            default -> throw new IllegalArgumentException("Unsupported workflow event type: " + event.eventType());
        };
    }

    private String notesFor(DomainEvent event) {
        return switch (event.eventType()) {
            case EventTypes.INVENTORY_RESERVED -> "Inventory reserved";
            case EventTypes.INVENTORY_RESERVATION_FAILED -> "Inventory reservation failed: "
                    + payload(event, InventoryReservationFailedPayload.class).reason();
            case EventTypes.PAYMENT_PROCESSED -> "Payment processed";
            case EventTypes.PAYMENT_FAILED -> "Payment failed: " + payload(event, PaymentFailedPayload.class).reason();
            case EventTypes.ORDER_SHIPPED -> "Order shipped with tracking number "
                    + payload(event, OrderShippedPayload.class).trackingNumber();
            default -> "Workflow event processed: " + event.eventType();
        };
    }

    private <T> T payload(DomainEvent event, Class<T> type) {
        try {
            return objectMapper.treeToValue(event.payload(), type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid event payload for " + event.eventType(), exception);
        }
    }

    private OrderItem toEntity(UUID orderId, OrderRequestItem item) {
        BigDecimal totalPrice = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
        return new OrderItem(
                UUID.randomUUID(),
                orderId,
                item.productId(),
                item.productName(),
                item.unitPrice(),
                item.quantity(),
                totalPrice
        );
    }

    private BigDecimal total(List<OrderRequestItem> requestItems) {
        return requestItems.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
