package com.example.ecommerce.product.controller;

import com.example.ecommerce.product.dto.request.CreateProductRequest;
import com.example.ecommerce.product.dto.request.UpdateProductRequest;
import com.example.ecommerce.product.dto.response.PageResponse;
import com.example.ecommerce.product.dto.response.ProductDetailResponse;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Operation(
            summary = "Browse products",
            description = "Lists products, optionally filtering by category or search text."
    )
    @ApiResponse(responseCode = "200", description = "Products returned")
    @ApiResponse(responseCode = "400", description = "Invalid paging or sorting request")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sort
    ) {
        return service.list(categoryId, search, page, size, sort);
    }

    @Operation(
            summary = "Search products",
            description = "Searches product names and SKUs using the same matching logic as product browsing."
    )
    @ApiResponse(responseCode = "200", description = "Products returned")
    @ApiResponse(responseCode = "400", description = "Invalid search request")
    @GetMapping("/search")
    public PageResponse<ProductResponse> search(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return service.list(null, query, page, size, "name");
    }

    @Operation(summary = "Get product details", description = "Returns a single product with category details.")
    @ApiResponse(responseCode = "200", description = "Product returned")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{productId}")
    public ProductDetailResponse get(@PathVariable UUID productId) {
        return service.get(productId);
    }

    @Operation(summary = "Create product", description = "Creates a catalog product.")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @PostMapping
    public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Update product", description = "Updates an existing catalog product.")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Product or category not found")
    @PutMapping("/{productId}")
    public ProductDetailResponse update(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return service.update(productId, request);
    }
}
