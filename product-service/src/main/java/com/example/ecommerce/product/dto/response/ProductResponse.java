package com.example.ecommerce.product.dto.response;
import java.math.BigDecimal; import java.util.UUID;
public record ProductResponse(UUID id,String name,String sku,BigDecimal price,UUID categoryId,String categoryName,boolean active) {}
