package com.example.ecommerce.product.controller;

import com.example.ecommerce.product.dto.response.CategoryResponse;
import com.example.ecommerce.product.dto.response.PageResponse;
import com.example.ecommerce.product.dto.response.ProductDetailResponse;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.exception.GlobalExceptionHandler;
import com.example.ecommerce.product.exception.ProductNotFoundException;
import com.example.ecommerce.product.service.CategoryService;
import com.example.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTest {
    MockMvc mvc;

    FakeProductService products;
    FakeCategoryService categories;

    @BeforeEach
    void setUp() {
        products = new FakeProductService();
        categories = new FakeCategoryService();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(products), new CategoryController(categories))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listingWorks() throws Exception {
        ProductResponse product = new ProductResponse(
                UUID.randomUUID(),
                "iPhone 15",
                "IPHONE-15",
                new BigDecimal("79999.00"),
                UUID.randomUUID(),
                "Mobiles",
                true
        );
        products.pageResponse = new PageResponse<>(List.of(product), 0, 20, 1, 1);

        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("iPhone 15"));
    }

    @Test
    void searchWorks() throws Exception {
        products.pageResponse = new PageResponse<>(List.of(), 0, 20, 0, 0);

        mvc.perform(get("/api/v1/products/search").param("query", "iphone"))
                .andExpect(status().isOk());
    }

    @Test
    void categoryFilterWorks() throws Exception {
        UUID id = UUID.randomUUID();
        products.pageResponse = new PageResponse<>(List.of(), 0, 20, 0, 0);

        mvc.perform(get("/api/v1/products").param("categoryId", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void productDetailWorks() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        products.detailResponse = new ProductDetailResponse(
                productId,
                "iPhone 15",
                "Apple smartphone",
                "IPHONE-15",
                new BigDecimal("79999.00"),
                categoryId,
                "Mobiles",
                true,
                Instant.parse("2026-06-24T10:00:00Z"),
                Instant.parse("2026-06-24T10:00:00Z")
        );

        mvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.categoryName").value("Mobiles"));
    }

    @Test
    void productNotFoundIs404() throws Exception {
        UUID id = UUID.randomUUID();
        products.exception = new ProductNotFoundException(id);

        mvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void categoriesWork() throws Exception {
        categories.responses = List.of(new CategoryResponse(UUID.randomUUID(), "Mobiles", "Phones"));

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mobiles"));
    }

    private static final class FakeProductService extends ProductService {
        private PageResponse<ProductResponse> pageResponse;
        private ProductDetailResponse detailResponse;
        private RuntimeException exception;

        private FakeProductService() {
            super(null, null, null);
        }

        @Override
        public PageResponse<ProductResponse> list(UUID categoryId, String search, int page, int size, String sort) {
            return pageResponse;
        }

        @Override
        public ProductDetailResponse get(UUID id) {
            if (exception != null) {
                throw exception;
            }
            return detailResponse;
        }
    }

    private static final class FakeCategoryService extends CategoryService {
        private List<CategoryResponse> responses = List.of();

        private FakeCategoryService() {
            super(null, null);
        }

        @Override
        public List<CategoryResponse> getAll() {
            return responses;
        }
    }
}
