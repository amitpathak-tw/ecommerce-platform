package com.example.ecommerce.cart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartSummaryResponse(
        UUID cartId,
        UUID customerId,
        int totalItems,
        BigDecimal totalAmount
) {
}
