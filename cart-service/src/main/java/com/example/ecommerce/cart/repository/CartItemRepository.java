package com.example.ecommerce.cart.repository;

import com.example.ecommerce.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);
}
