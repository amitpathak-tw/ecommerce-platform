package com.example.ecommerce.inventory.repository;

import com.example.ecommerce.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByProductId(UUID productId);

    List<InventoryItem> findByProductIdIn(Collection<UUID> productIds);
}
