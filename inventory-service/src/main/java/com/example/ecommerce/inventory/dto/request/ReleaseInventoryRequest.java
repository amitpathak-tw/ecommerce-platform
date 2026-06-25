package com.example.ecommerce.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReleaseInventoryRequest(
        @NotEmpty List<@Valid InventoryRequestItem> items
) {
}
