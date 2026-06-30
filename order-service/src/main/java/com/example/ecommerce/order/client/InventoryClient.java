package com.example.ecommerce.order.client;

import com.example.ecommerce.order.dto.request.OrderRequestItem;
import com.example.ecommerce.order.exception.DependentServiceUnavailableException;
import com.example.ecommerce.order.exception.OrderPlacementException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Component
public class InventoryClient {
    private final RestClient inventoryRestClient;

    public InventoryClient(RestClient inventoryRestClient) {
        this.inventoryRestClient = inventoryRestClient;
    }

    @Retry(name = "inventoryServiceRetry")
    @CircuitBreaker(name = "inventoryServiceCircuitBreaker", fallbackMethod = "reserveFallback")
    public void reserve(List<OrderRequestItem> items) {
        try {
            inventoryRestClient.post()
                    .uri("/api/v1/inventory/reserve")
                    .body(new ReserveInventoryRequest(items.stream()
                            .map(item -> new InventoryRequestItem(item.productId(), item.quantity()))
                            .toList()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new OrderPlacementException("Inventory reservation failed");
                    })
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new OrderPlacementException("Inventory reservation failed", exception);
        }
    }

    private void reserveFallback(List<OrderRequestItem> items, Throwable exception) {
        throw new DependentServiceUnavailableException(
                "Inventory service is temporarily unavailable. Please try again later.",
                exception
        );
    }

    private record ReserveInventoryRequest(List<InventoryRequestItem> items) {
    }

    private record InventoryRequestItem(UUID productId, int quantity) {
    }
}
