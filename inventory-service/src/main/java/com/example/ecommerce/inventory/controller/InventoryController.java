package com.example.ecommerce.inventory.controller;

import com.example.ecommerce.inventory.dto.request.ReleaseInventoryRequest;
import com.example.ecommerce.inventory.dto.request.ReserveInventoryRequest;
import com.example.ecommerce.inventory.dto.response.InventoryAvailabilityResponse;
import com.example.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @Operation(summary = "Get product availability", description = "Returns stock information for one product.")
    @ApiResponse(responseCode = "200", description = "Inventory returned")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    @GetMapping("/{productId}")
    public InventoryAvailabilityResponse getAvailability(@PathVariable UUID productId) {
        return service.getAvailability(productId);
    }

    @Operation(summary = "Get product availability in bulk", description = "Returns stock information for products.")
    @ApiResponse(responseCode = "200", description = "Inventory returned")
    @ApiResponse(responseCode = "400", description = "Invalid product IDs")
    @GetMapping("/availability")
    public List<InventoryAvailabilityResponse> getAvailability(
            @RequestParam @NotEmpty List<UUID> productIds
    ) {
        return service.getAvailability(productIds);
    }

    @Operation(summary = "Reserve inventory", description = "Atomically reserves stock for order placement.")
    @ApiResponse(responseCode = "204", description = "Inventory reserved")
    @ApiResponse(responseCode = "409", description = "Insufficient stock")
    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@Valid @RequestBody ReserveInventoryRequest request) {
        service.reserve(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Release inventory", description = "Atomically releases previously reserved stock.")
    @ApiResponse(responseCode = "204", description = "Inventory released")
    @ApiResponse(responseCode = "409", description = "Release would make reserved stock negative")
    @PostMapping("/release")
    public ResponseEntity<Void> release(@Valid @RequestBody ReleaseInventoryRequest request) {
        service.release(request);
        return ResponseEntity.noContent().build();
    }
}
