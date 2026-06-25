package com.example.ecommerce.inventory.mapper;

import com.example.ecommerce.inventory.dto.response.InventoryAvailabilityResponse;
import com.example.ecommerce.inventory.entity.InventoryItem;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public InventoryAvailabilityResponse toResponse(InventoryItem item) {
        return new InventoryAvailabilityResponse(
                item.getProductId(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getAvailableQuantity() > 0
        );
    }
}
