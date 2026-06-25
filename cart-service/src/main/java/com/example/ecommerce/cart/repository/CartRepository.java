package com.example.ecommerce.cart.repository;

import com.example.ecommerce.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
}
