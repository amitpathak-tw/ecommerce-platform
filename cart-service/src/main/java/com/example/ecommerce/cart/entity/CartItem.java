package com.example.ecommerce.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID cartId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 150)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CartItem() {
    }

    public CartItem(
            UUID id,
            UUID cartId,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.cartId = cartId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void increaseQuantity(int quantity, Instant updatedAt) {
        this.quantity += quantity;
        this.updatedAt = updatedAt;
    }

    public void updateQuantity(int quantity, Instant updatedAt) {
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCartId() {
        return cartId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }
}
