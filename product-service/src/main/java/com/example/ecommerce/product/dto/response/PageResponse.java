package com.example.ecommerce.product.dto.response;
import java.util.List;
public record PageResponse<T>(List<T> content,int page,int size,long totalElements,int totalPages) {}
