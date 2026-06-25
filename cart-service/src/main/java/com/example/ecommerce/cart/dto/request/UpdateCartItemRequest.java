package com.example.ecommerce.cart.dto.request;

import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(@Positive int quantity) {
}
