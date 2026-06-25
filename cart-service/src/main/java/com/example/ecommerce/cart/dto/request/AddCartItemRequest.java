package com.example.ecommerce.cart.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID productId,
        @NotBlank String productName,
        @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
        @Positive int quantity
) {
}
