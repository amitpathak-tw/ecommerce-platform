package com.example.ecommerce.product.controller;

import com.example.ecommerce.product.dto.response.CategoryResponse;
import com.example.ecommerce.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Operation(summary = "List product categories", description = "Returns all product categories.")
    @ApiResponse(responseCode = "200", description = "Categories returned")
    @GetMapping
    public List<CategoryResponse> list() {
        return service.getAll();
    }
}
