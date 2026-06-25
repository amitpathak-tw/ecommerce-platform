package com.example.ecommerce.product.dto.response;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public record ProductDetailResponse(UUID id,String name,String description,String sku,BigDecimal price,UUID categoryId,String categoryName,boolean active,Instant createdAt,Instant updatedAt) {}
