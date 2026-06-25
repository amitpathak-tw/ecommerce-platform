package com.example.ecommerce.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice
) {
}
