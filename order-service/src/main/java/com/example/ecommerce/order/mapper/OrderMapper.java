package com.example.ecommerce.order.mapper;

import com.example.ecommerce.order.dto.response.OrderHistoryResponse;
import com.example.ecommerce.order.dto.response.OrderItemResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderItem;
import com.example.ecommerce.order.entity.OrderStatusHistory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus().name(),
                items.stream().map(this::toResponse).toList(),
                order.getCreatedAt(),
                message(order.getStatus().name())
        );
    }

    public OrderHistoryResponse toResponse(OrderStatusHistory history) {
        return new OrderHistoryResponse(
                history.getOrderId(),
                history.getStatus().name(),
                history.getNotes(),
                history.getCreatedAt()
        );
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }

    private String message(String status) {
        if ("CREATED".equals(status)) {
            return "Order accepted for asynchronous processing";
        }
        if ("CANCELLED".equals(status)) {
            return "Order workflow was cancelled";
        }
        if ("SHIPPED".equals(status) || "CONFIRMED".equals(status)) {
            return "Order workflow completed successfully";
        }
        return "Order workflow is in progress";
    }
}
