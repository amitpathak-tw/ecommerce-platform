package com.example.ecommerce.product.mapper;

import com.example.ecommerce.product.dto.response.*;
import com.example.ecommerce.product.entity.*;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductResponse toResponse(Product p, Category c) { return new ProductResponse(p.getId(),p.getName(),p.getSku(),p.getPrice(),p.getCategoryId(),c.getName(),p.isActive()); }
    public ProductDetailResponse toDetail(Product p, Category c) { return new ProductDetailResponse(p.getId(),p.getName(),p.getDescription(),p.getSku(),p.getPrice(),p.getCategoryId(),c.getName(),p.isActive(),p.getCreatedAt(),p.getUpdatedAt()); }
    public CategoryResponse toResponse(Category c) { return new CategoryResponse(c.getId(), c.getName(), c.getDescription()); }
}
