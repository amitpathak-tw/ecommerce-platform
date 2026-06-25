package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.request.AddCartItemRequest;
import com.example.ecommerce.cart.dto.request.CreateCartRequest;
import com.example.ecommerce.cart.dto.request.UpdateCartItemRequest;
import com.example.ecommerce.cart.dto.response.CartResponse;
import com.example.ecommerce.cart.dto.response.CartSummaryResponse;
import com.example.ecommerce.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @Operation(summary = "Create cart", description = "Creates an ACTIVE cart for a customer.")
    @ApiResponse(responseCode = "201", description = "Cart created")
    @PostMapping
    public ResponseEntity<CartResponse> create(@Valid @RequestBody CreateCartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Get cart", description = "Returns a cart with all items.")
    @ApiResponse(responseCode = "200", description = "Cart returned")
    @ApiResponse(responseCode = "404", description = "Cart not found")
    @GetMapping("/{cartId}")
    public CartResponse get(@PathVariable UUID cartId) {
        return service.get(cartId);
    }

    @Operation(summary = "Add cart item", description = "Adds an item or increases quantity for an existing product.")
    @ApiResponse(responseCode = "201", description = "Item added")
    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable UUID cartId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addItem(cartId, request));
    }

    @Operation(summary = "Update cart item", description = "Updates a cart item quantity.")
    @ApiResponse(responseCode = "200", description = "Item updated")
    @PutMapping("/{cartId}/items/{itemId}")
    public CartResponse updateItem(
            @PathVariable UUID cartId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return service.updateItem(cartId, itemId, request);
    }

    @Operation(summary = "Remove cart item", description = "Removes an item from a cart.")
    @ApiResponse(responseCode = "204", description = "Item removed")
    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable UUID cartId, @PathVariable UUID itemId) {
        service.removeItem(cartId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get cart summary", description = "Returns total item count and amount.")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @GetMapping("/{cartId}/summary")
    public CartSummaryResponse summary(@PathVariable UUID cartId) {
        return service.summary(cartId);
    }
}
