package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.request.PlaceOrderRequest;
import com.example.ecommerce.order.dto.response.OrderHistoryResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusResponse;
import com.example.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "Place order", description = "Reserves inventory and creates a confirmed order.")
    @ApiResponse(responseCode = "201", description = "Order confirmed")
    @ApiResponse(responseCode = "409", description = "Order placement failed")
    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.place(request));
    }

    @Operation(summary = "Get order", description = "Returns order details.")
    @ApiResponse(responseCode = "200", description = "Order returned")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable UUID orderId) {
        return service.get(orderId);
    }

    @Operation(summary = "Get order status", description = "Returns the current order status.")
    @GetMapping("/{orderId}/status")
    public OrderStatusResponse status(@PathVariable UUID orderId) {
        return service.status(orderId);
    }

    @Operation(summary = "Get customer orders", description = "Returns a customer's order history.")
    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> customerOrders(@PathVariable UUID customerId) {
        return service.customerOrders(customerId);
    }

    @Operation(summary = "Get order status history", description = "Returns status timeline entries.")
    @GetMapping("/{orderId}/history")
    public List<OrderHistoryResponse> history(@PathVariable UUID orderId) {
        return service.history(orderId);
    }
}
