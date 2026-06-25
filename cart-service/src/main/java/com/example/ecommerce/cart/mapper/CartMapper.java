package com.example.ecommerce.cart.mapper;

import com.example.ecommerce.cart.dto.response.CartItemResponse;
import com.example.ecommerce.cart.dto.response.CartResponse;
import com.example.ecommerce.cart.dto.response.CartSummaryResponse;
import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CartMapper {
    public CartResponse toResponse(Cart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toResponse)
                .toList();
        return new CartResponse(
                cart.getId(),
                cart.getCustomerId(),
                cart.getStatus().name(),
                itemResponses,
                totalAmount(items)
        );
    }

    public CartSummaryResponse toSummary(Cart cart, List<CartItem> items) {
        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
        return new CartSummaryResponse(cart.getId(), cart.getCustomerId(), totalItems, totalAmount(items));
    }

    private CartItemResponse toResponse(CartItem item) {
        BigDecimal totalPrice = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                totalPrice
        );
    }

    private BigDecimal totalAmount(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
