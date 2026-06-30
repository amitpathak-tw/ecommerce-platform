package com.example.ecommerce.inventory.service;

import com.example.ecommerce.inventory.dto.request.InventoryRequestItem;
import com.example.ecommerce.inventory.dto.request.ReleaseInventoryRequest;
import com.example.ecommerce.inventory.dto.request.ReserveInventoryRequest;
import com.example.ecommerce.inventory.dto.response.InventoryAvailabilityResponse;
import com.example.ecommerce.inventory.entity.InventoryItem;
import com.example.ecommerce.inventory.entity.ProcessedEvent;
import com.example.ecommerce.inventory.event.DomainEvent;
import com.example.ecommerce.inventory.event.EventTypes;
import com.example.ecommerce.inventory.event.payload.InventoryReservationFailedItemPayload;
import com.example.ecommerce.inventory.event.payload.InventoryReservationFailedPayload;
import com.example.ecommerce.inventory.event.payload.InventoryReservedItemPayload;
import com.example.ecommerce.inventory.event.payload.InventoryReservedPayload;
import com.example.ecommerce.inventory.event.payload.OrderPlacedPayload;
import com.example.ecommerce.inventory.exception.InsufficientStockException;
import com.example.ecommerce.inventory.exception.InventoryNotFoundException;
import com.example.ecommerce.inventory.mapper.InventoryMapper;
import com.example.ecommerce.inventory.repository.InventoryItemRepository;
import com.example.ecommerce.inventory.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String SOURCE_SERVICE = "inventory-service";

    private final InventoryItemRepository repository;
    private final InventoryMapper mapper;
    private final ProcessedEventRepository processedEvents;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Autowired
    public InventoryService(
            InventoryItemRepository repository,
            InventoryMapper mapper,
            ProcessedEventRepository processedEvents,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    protected InventoryService(InventoryItemRepository repository, InventoryMapper mapper) {
        this(repository, mapper, null, null, null);
    }

    public InventoryAvailabilityResponse getAvailability(UUID productId) {
        return mapper.toResponse(require(productId));
    }

    public List<InventoryAvailabilityResponse> getAvailability(List<UUID> productIds) {
        return repository.findByProductIdIn(productIds)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void reserve(ReserveInventoryRequest request) {
        for (InventoryRequestItem requestItem : request.items()) {
            InventoryItem inventoryItem = require(requestItem.productId());
            if (inventoryItem.getAvailableQuantity() < requestItem.quantity()) {
                throw new InsufficientStockException(requestItem.productId());
            }
            inventoryItem.reserve(requestItem.quantity(), Instant.now());
        }
    }

    @Transactional
    public void release(ReleaseInventoryRequest request) {
        for (InventoryRequestItem requestItem : request.items()) {
            InventoryItem inventoryItem = require(requestItem.productId());
            if (inventoryItem.getReservedQuantity() < requestItem.quantity()) {
                throw new InsufficientStockException(
                        "Cannot release more reserved stock than exists for product: " + requestItem.productId()
                );
            }
            inventoryItem.release(requestItem.quantity(), Instant.now());
        }
    }

    @Transactional
    public Optional<DomainEvent> reserveForOrder(DomainEvent sourceEvent, OrderPlacedPayload payload) {
        if (processedEvents.existsById(sourceEvent.eventId())) {
            log.info(
                    "correlationId={} eventId={} orderId={} service=inventory-service eventType={} message=\"Duplicate event skipped\"",
                    sourceEvent.correlationId(),
                    sourceEvent.eventId(),
                    sourceEvent.aggregateId(),
                    sourceEvent.eventType()
            );
            return Optional.empty();
        }
        if (sourceEvent.payload().path("simulateConsumerFailure").asBoolean(false)) {
            throw new IllegalStateException("Simulated inventory consumer failure");
        }

        List<InventoryReservationFailedItemPayload> failedItems = unavailableItems(payload);
        DomainEvent result;
        if (failedItems.isEmpty()) {
            Instant reservedAt = Instant.now();
            for (var item : payload.items()) {
                repository.findByProductId(item.productId())
                        .orElseThrow(() -> new InventoryNotFoundException(item.productId()))
                        .reserve(item.quantity(), reservedAt);
            }
            meterRegistry.counter("inventory.reserved.count").increment();
            result = event(
                    EventTypes.INVENTORY_RESERVED,
                    sourceEvent,
                    new InventoryReservedPayload(
                            payload.orderId(),
                            payload.customerId(),
                            payload.items().stream()
                                    .map(item -> new InventoryReservedItemPayload(item.productId(), item.quantity()))
                                    .toList(),
                            reservedAt,
                            payload.totalAmount()
                    )
            );
        } else {
            result = event(
                    EventTypes.INVENTORY_RESERVATION_FAILED,
                    sourceEvent,
                    new InventoryReservationFailedPayload(
                            payload.orderId(),
                            "Insufficient stock",
                            failedItems
                    )
            );
        }

        processedEvents.save(new ProcessedEvent(
                sourceEvent.eventId(),
                sourceEvent.eventType(),
                sourceEvent.aggregateId(),
                Instant.now()
        ));
        return Optional.of(result);
    }

    private InventoryItem require(UUID productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    private List<InventoryReservationFailedItemPayload> unavailableItems(OrderPlacedPayload payload) {
        List<InventoryReservationFailedItemPayload> failedItems = new ArrayList<>();
        for (var item : payload.items()) {
            int available = repository.findByProductId(item.productId())
                    .map(InventoryItem::getAvailableQuantity)
                    .orElse(0);
            if (available < item.quantity()) {
                failedItems.add(new InventoryReservationFailedItemPayload(
                        item.productId(),
                        item.quantity(),
                        available
                ));
            }
        }
        return failedItems;
    }

    private DomainEvent event(String eventType, DomainEvent sourceEvent, Object payload) {
        return new DomainEvent(
                UUID.randomUUID(),
                eventType,
                sourceEvent.aggregateId(),
                sourceEvent.correlationId(),
                Instant.now(),
                SOURCE_SERVICE,
                objectMapper.valueToTree(payload)
        );
    }
}
