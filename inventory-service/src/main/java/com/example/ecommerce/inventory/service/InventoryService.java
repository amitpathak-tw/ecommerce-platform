package com.example.ecommerce.inventory.service;

import com.example.ecommerce.inventory.dto.request.InventoryRequestItem;
import com.example.ecommerce.inventory.dto.request.ReleaseInventoryRequest;
import com.example.ecommerce.inventory.dto.request.ReserveInventoryRequest;
import com.example.ecommerce.inventory.dto.response.InventoryAvailabilityResponse;
import com.example.ecommerce.inventory.entity.InventoryItem;
import com.example.ecommerce.inventory.exception.InsufficientStockException;
import com.example.ecommerce.inventory.exception.InventoryNotFoundException;
import com.example.ecommerce.inventory.mapper.InventoryMapper;
import com.example.ecommerce.inventory.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {
    private final InventoryItemRepository repository;
    private final InventoryMapper mapper;

    public InventoryService(InventoryItemRepository repository, InventoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
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

    private InventoryItem require(UUID productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }
}
