package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.request.PlaceOrderRequest;
import com.example.ecommerce.order.dto.response.OrderHistoryResponse;
import com.example.ecommerce.order.dto.response.OrderItemResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusResponse;
import com.example.ecommerce.order.exception.GlobalExceptionHandler;
import com.example.ecommerce.order.exception.OrderNotFoundException;
import com.example.ecommerce.order.exception.OrderPlacementException;
import com.example.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {
    private MockMvc mvc;
    private FakeOrderService service;

    @BeforeEach
    void setUp() {
        service = new FakeOrderService();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void placeOrderSucceedsWhenInventoryIsAvailable() throws Exception {
        UUID orderId = UUID.randomUUID();
        service.response = orderResponse(orderId, UUID.randomUUID());

        mvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void placeOrderFailsWhenInventoryIsUnavailable() throws Exception {
        service.exception = new OrderPlacementException("Inventory reservation failed");

        mvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_PLACEMENT_FAILED"));
    }

    @Test
    void getOrderByIdWorks() throws Exception {
        UUID orderId = UUID.randomUUID();
        service.response = orderResponse(orderId, UUID.randomUUID());

        mvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void getOrderStatusWorks() throws Exception {
        UUID orderId = UUID.randomUUID();
        service.statusResponse = new OrderStatusResponse(orderId, "CONFIRMED");

        mvc.perform(get("/api/v1/orders/{orderId}/status", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getCustomerOrderHistoryWorks() throws Exception {
        UUID customerId = UUID.randomUUID();
        service.responses = List.of(orderResponse(UUID.randomUUID(), customerId));

        mvc.perform(get("/api/v1/orders/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()));
    }

    @Test
    void getOrderStatusHistoryWorks() throws Exception {
        UUID orderId = UUID.randomUUID();
        service.history = List.of(new OrderHistoryResponse(
                orderId,
                "CONFIRMED",
                "Order confirmed successfully",
                Instant.parse("2026-06-24T10:00:00Z")
        ));

        mvc.perform(get("/api/v1/orders/{orderId}/history", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void orderNotFoundReturns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        service.exception = new OrderNotFoundException(orderId);

        mvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    }

    private static String orderRequest() {
        return """
                {
                  "customerId": "30000000-0000-0000-0000-000000000001",
                  "items": [
                    {
                      "productId": "10000000-0000-0000-0000-000000000001",
                      "productName": "iPhone 15",
                      "unitPrice": 79999.00,
                      "quantity": 2
                    }
                  ]
                }
                """;
    }

    private static OrderResponse orderResponse(UUID orderId, UUID customerId) {
        return new OrderResponse(
                orderId,
                customerId,
                new BigDecimal("159998.00"),
                "CONFIRMED",
                List.of(new OrderItemResponse(
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        "iPhone 15",
                        new BigDecimal("79999.00"),
                        2,
                        new BigDecimal("159998.00")
                )),
                Instant.parse("2026-06-24T10:00:00Z")
        );
    }

    private static final class FakeOrderService extends OrderService {
        private OrderResponse response;
        private List<OrderResponse> responses = List.of();
        private OrderStatusResponse statusResponse;
        private List<OrderHistoryResponse> history = List.of();
        private RuntimeException exception;

        private FakeOrderService() {
            super(null, null, null, null, null);
        }

        @Override
        public OrderResponse place(PlaceOrderRequest request) {
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        @Override
        public OrderResponse get(UUID orderId) {
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        @Override
        public OrderStatusResponse status(UUID orderId) {
            return statusResponse;
        }

        @Override
        public List<OrderResponse> customerOrders(UUID customerId) {
            return responses;
        }

        @Override
        public List<OrderHistoryResponse> history(UUID orderId) {
            return history;
        }
    }
}
