package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.request.AddCartItemRequest;
import com.example.ecommerce.cart.dto.request.CreateCartRequest;
import com.example.ecommerce.cart.dto.request.UpdateCartItemRequest;
import com.example.ecommerce.cart.dto.response.CartItemResponse;
import com.example.ecommerce.cart.dto.response.CartResponse;
import com.example.ecommerce.cart.dto.response.CartSummaryResponse;
import com.example.ecommerce.cart.exception.CartNotFoundException;
import com.example.ecommerce.cart.exception.GlobalExceptionHandler;
import com.example.ecommerce.cart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerTest {
    private MockMvc mvc;
    private FakeCartService service;

    @BeforeEach
    void setUp() {
        service = new FakeCartService();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new CartController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createCartWorks() throws Exception {
        UUID customerId = UUID.randomUUID();
        service.response = cartResponse(UUID.randomUUID(), customerId, List.of());

        mvc.perform(post("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getCartWorks() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        service.response = cartResponse(cartId, customerId, List.of(itemResponse(2)));

        mvc.perform(get("/api/v1/carts/{cartId}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(159998.00));
    }

    @Test
    void addItemToCartWorks() throws Exception {
        UUID cartId = UUID.randomUUID();
        service.response = cartResponse(cartId, UUID.randomUUID(), List.of(itemResponse(2)));

        mvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemRequest(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].productName").value("iPhone 15"));
    }

    @Test
    void addingSameProductIncreasesQuantity() throws Exception {
        UUID cartId = UUID.randomUUID();
        service.response = cartResponse(cartId, UUID.randomUUID(), List.of(itemResponse(4)));

        mvc.perform(post("/api/v1/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemRequest(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    @Test
    void updateCartItemQuantityWorks() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        service.response = cartResponse(cartId, UUID.randomUUID(), List.of(itemResponse(3)));

        mvc.perform(put("/api/v1/carts/{cartId}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    void removeCartItemWorks() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mvc.perform(delete("/api/v1/carts/{cartId}/items/{itemId}", cartId, itemId))
                .andExpect(status().isNoContent());

        assertThat(service.removeCalled).isTrue();
    }

    @Test
    void cartSummaryWorks() throws Exception {
        UUID cartId = UUID.randomUUID();
        service.summary = new CartSummaryResponse(cartId, UUID.randomUUID(), 3, new BigDecimal("239997.00"));

        mvc.perform(get("/api/v1/carts/{cartId}/summary", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalAmount").value(239997.00));
    }

    @Test
    void cartNotFoundReturns404() throws Exception {
        UUID cartId = UUID.randomUUID();
        service.exception = new CartNotFoundException(cartId);

        mvc.perform(get("/api/v1/carts/{cartId}", cartId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CART_NOT_FOUND"));
    }

    private static String itemRequest(int quantity) {
        return """
                {
                  "productId": "10000000-0000-0000-0000-000000000001",
                  "productName": "iPhone 15",
                  "unitPrice": 79999.00,
                  "quantity": %d
                }
                """.formatted(quantity);
    }

    private static CartResponse cartResponse(UUID cartId, UUID customerId, List<CartItemResponse> items) {
        BigDecimal total = items.stream()
                .map(CartItemResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cartId, customerId, "ACTIVE", new ArrayList<>(items), total);
    }

    private static CartItemResponse itemResponse(int quantity) {
        BigDecimal unitPrice = new BigDecimal("79999.00");
        return new CartItemResponse(
                UUID.randomUUID(),
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "iPhone 15",
                unitPrice,
                quantity,
                unitPrice.multiply(BigDecimal.valueOf(quantity))
        );
    }

    private static final class FakeCartService extends CartService {
        private CartResponse response;
        private CartSummaryResponse summary;
        private RuntimeException exception;
        private boolean removeCalled;

        private FakeCartService() {
            super(null, null, null);
        }

        @Override
        public CartResponse create(CreateCartRequest request) {
            return response;
        }

        @Override
        public CartResponse get(UUID cartId) {
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        @Override
        public CartResponse addItem(UUID cartId, AddCartItemRequest request) {
            return response;
        }

        @Override
        public CartResponse updateItem(UUID cartId, UUID itemId, UpdateCartItemRequest request) {
            return response;
        }

        @Override
        public void removeItem(UUID cartId, UUID itemId) {
            removeCalled = true;
        }

        @Override
        public CartSummaryResponse summary(UUID cartId) {
            return summary;
        }
    }
}
