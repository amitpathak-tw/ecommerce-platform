package com.example.ecommerce.order.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        BigDecimal totalAmount,
        String status,
        List<OrderItemResponse> items,
        Instant createdAt,
        String message
) {
    public OrderResponse(
            UUID id,
            UUID customerId,
            BigDecimal totalAmount,
            String status,
            List<OrderItemResponse> items,
            Instant createdAt
    ) {
        this(id, customerId, totalAmount, status, items, createdAt, null);
    }
}
