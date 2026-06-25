package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.request.AddCartItemRequest;
import com.example.ecommerce.cart.dto.request.CreateCartRequest;
import com.example.ecommerce.cart.dto.request.UpdateCartItemRequest;
import com.example.ecommerce.cart.dto.response.CartResponse;
import com.example.ecommerce.cart.dto.response.CartSummaryResponse;
import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.cart.entity.CartStatus;
import com.example.ecommerce.cart.exception.CartItemNotFoundException;
import com.example.ecommerce.cart.exception.CartNotFoundException;
import com.example.ecommerce.cart.exception.CartStateException;
import com.example.ecommerce.cart.mapper.CartMapper;
import com.example.ecommerce.cart.repository.CartItemRepository;
import com.example.ecommerce.cart.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {
    private final CartRepository carts;
    private final CartItemRepository items;
    private final CartMapper mapper;

    public CartService(CartRepository carts, CartItemRepository items, CartMapper mapper) {
        this.carts = carts;
        this.items = items;
        this.mapper = mapper;
    }

    @Transactional
    public CartResponse create(CreateCartRequest request) {
        Instant now = Instant.now();
        Cart cart = carts.save(new Cart(UUID.randomUUID(), request.customerId(), CartStatus.ACTIVE, now, now));
        return mapper.toResponse(cart, List.of());
    }

    public CartResponse get(UUID cartId) {
        Cart cart = requireCart(cartId);
        return mapper.toResponse(cart, items.findByCartId(cartId));
    }

    @Transactional
    public CartResponse addItem(UUID cartId, AddCartItemRequest request) {
        Cart cart = requireActiveCart(cartId);
        Instant now = Instant.now();
        items.findByCartIdAndProductId(cartId, request.productId())
                .ifPresentOrElse(
                        item -> item.increaseQuantity(request.quantity(), now),
                        () -> items.save(new CartItem(
                                UUID.randomUUID(),
                                cartId,
                                request.productId(),
                                request.productName(),
                                request.unitPrice(),
                                request.quantity(),
                                now,
                                now
                        ))
                );
        cart.touch(now);
        return mapper.toResponse(cart, items.findByCartId(cartId));
    }

    @Transactional
    public CartResponse updateItem(UUID cartId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = requireActiveCart(cartId);
        CartItem item = items.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
        Instant now = Instant.now();
        item.updateQuantity(request.quantity(), now);
        cart.touch(now);
        return mapper.toResponse(cart, items.findByCartId(cartId));
    }

    @Transactional
    public void removeItem(UUID cartId, UUID itemId) {
        Cart cart = requireActiveCart(cartId);
        CartItem item = items.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
        items.delete(item);
        cart.touch(Instant.now());
    }

    public CartSummaryResponse summary(UUID cartId) {
        Cart cart = requireCart(cartId);
        return mapper.toSummary(cart, items.findByCartId(cartId));
    }

    private Cart requireCart(UUID cartId) {
        return carts.findById(cartId).orElseThrow(() -> new CartNotFoundException(cartId));
    }

    private Cart requireActiveCart(UUID cartId) {
        Cart cart = requireCart(cartId);
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new CartStateException("Cart must be ACTIVE");
        }
        return cart;
    }
}
