package com.example.ecommerce.product.service;

import com.example.ecommerce.product.dto.request.CreateProductRequest;
import com.example.ecommerce.product.dto.request.UpdateProductRequest;
import com.example.ecommerce.product.dto.response.PageResponse;
import com.example.ecommerce.product.dto.response.ProductDetailResponse;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.entity.Category;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.exception.ProductNotFoundException;
import com.example.ecommerce.product.mapper.ProductMapper;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository products;
    private final CategoryService categories;
    private final ProductMapper mapper;

    public ProductService(ProductRepository products, CategoryService categories, ProductMapper mapper) {
        this.products = products;
        this.categories = categories;
        this.mapper = mapper;
    }

    public PageResponse<ProductResponse> list(UUID categoryId, String search, int page, int size, String sort) {
        if (categoryId != null) {
            categories.require(categoryId);
        }
        String safeSort = switch (sort) {
            case "name", "price", "sku" -> sort;
            default -> "name";
        };
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(safeSort).ascending());
        Page<Product> result = findProducts(categoryId, blankToNull(search), pageRequest);

        return new PageResponse<>(
                result.getContent()
                        .stream()
                        .map(product -> mapper.toResponse(product, categories.require(product.getCategoryId())))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public ProductDetailResponse get(UUID id) {
        Product product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        return mapper.toDetail(product, categories.require(product.getCategoryId()));
    }

    public ProductDetailResponse create(CreateProductRequest request) {
        Category category = categories.require(request.categoryId());
        Instant now = Instant.now();
        Product product = products.save(new Product(
                UUID.randomUUID(),
                request.name(),
                request.description(),
                request.sku(),
                request.price(),
                request.categoryId(),
                request.active(),
                now,
                now
        ));
        return mapper.toDetail(product, category);
    }

    public ProductDetailResponse update(UUID id, UpdateProductRequest request) {
        Product product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        Category category = categories.require(request.categoryId());
        product.update(
                request.name(),
                request.description(),
                request.sku(),
                request.price(),
                request.categoryId(),
                request.active(),
                Instant.now()
        );
        return mapper.toDetail(products.save(product), category);
    }

    private Page<Product> findProducts(UUID categoryId, String search, PageRequest pageRequest) {
        if (categoryId != null && search != null) {
            return products.searchByCategoryIdAndTerm(categoryId, search, pageRequest);
        }
        if (categoryId != null) {
            return products.findByCategoryId(categoryId, pageRequest);
        }
        if (search != null) {
            return products.searchByTerm(search, pageRequest);
        }
        return products.findAll(pageRequest);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
