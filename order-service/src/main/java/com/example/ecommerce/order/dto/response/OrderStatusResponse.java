package com.example.ecommerce.order.dto.response;

import java.util.UUID;

public record OrderStatusResponse(UUID orderId, String status) {
}
