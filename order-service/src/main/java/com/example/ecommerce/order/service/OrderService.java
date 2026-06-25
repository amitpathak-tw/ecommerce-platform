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
import com.example.ecommerce.order.exception.OrderNotFoundException;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final OrderStatusHistoryRepository history;
    private final InventoryClient inventoryClient;
    private final OrderMapper mapper;

    public OrderService(
            OrderRepository orders,
            OrderItemRepository items,
            OrderStatusHistoryRepository history,
            InventoryClient inventoryClient,
            OrderMapper mapper
    ) {
        this.orders = orders;
        this.items = items;
        this.history = history;
        this.inventoryClient = inventoryClient;
        this.mapper = mapper;
    }

    @Transactional
    public OrderResponse place(PlaceOrderRequest request) {
        inventoryClient.reserve(request.items());
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();
        BigDecimal totalAmount = total(request.items());
        Order order = orders.save(new Order(orderId, request.customerId(), totalAmount, OrderStatus.CONFIRMED, now, now));
        List<OrderItem> orderItems = request.items().stream()
                .map(item -> toEntity(orderId, item))
                .toList();
        items.saveAll(orderItems);
        history.save(new OrderStatusHistory(
                UUID.randomUUID(),
                orderId,
                OrderStatus.CONFIRMED,
                "Order confirmed successfully",
                now
        ));
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

    private Order require(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
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
