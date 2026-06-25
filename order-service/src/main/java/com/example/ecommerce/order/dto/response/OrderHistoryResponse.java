package com.example.ecommerce.order.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrderHistoryResponse(
        UUID orderId,
        String status,
        String notes,
        Instant createdAt
) {
}
