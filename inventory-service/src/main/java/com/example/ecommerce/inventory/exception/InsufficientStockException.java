package com.example.ecommerce.inventory.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID productId) {
        super("Insufficient stock for product: " + productId);
    }

    public InsufficientStockException(String message) {
        super(message);
    }
}
