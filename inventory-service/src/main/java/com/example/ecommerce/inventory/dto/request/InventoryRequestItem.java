package com.example.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record InventoryRequestItem(
        @NotNull UUID productId,
        @Positive int quantity
) {
}
