package com.example.ecommerce.product.dto.request;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.UUID;
public record UpdateProductRequest(@NotBlank String name, String description, @NotBlank String sku, @NotNull @DecimalMin("0.0") BigDecimal price, @NotNull UUID categoryId, boolean active) {}
