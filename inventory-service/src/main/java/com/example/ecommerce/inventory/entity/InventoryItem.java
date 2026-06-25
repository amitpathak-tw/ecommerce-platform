package com.example.ecommerce.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID productId;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public InventoryItem(
            UUID id,
            UUID productId,
            int availableQuantity,
            int reservedQuantity,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void reserve(int quantity, Instant updatedAt) {
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        this.updatedAt = updatedAt;
    }

    public void release(int quantity, Instant updatedAt) {
        availableQuantity += quantity;
        reservedQuantity -= quantity;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }
}
