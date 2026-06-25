package com.example.ecommerce.cart.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        String status,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
}
