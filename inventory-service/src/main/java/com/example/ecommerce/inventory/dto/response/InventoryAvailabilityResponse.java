package com.example.ecommerce.inventory.dto.response;

import java.util.UUID;

public record InventoryAvailabilityResponse(
        UUID productId,
        int availableQuantity,
        int reservedQuantity,
        boolean available
) {
}
