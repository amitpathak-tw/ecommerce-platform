package com.example.ecommerce.inventory.controller;

import com.example.ecommerce.inventory.dto.request.ReleaseInventoryRequest;
import com.example.ecommerce.inventory.dto.request.ReserveInventoryRequest;
import com.example.ecommerce.inventory.dto.response.InventoryAvailabilityResponse;
import com.example.ecommerce.inventory.exception.GlobalExceptionHandler;
import com.example.ecommerce.inventory.exception.InsufficientStockException;
import com.example.ecommerce.inventory.exception.InventoryNotFoundException;
import com.example.ecommerce.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryControllerTest {
    MockMvc mvc;

    FakeInventoryService service;

    @BeforeEach
    void setUp() {
        service = new FakeInventoryService();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new InventoryController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void singleProductAvailabilityWorks() throws Exception {
        UUID productId = UUID.randomUUID();
        service.singleResponse = new InventoryAvailabilityResponse(productId, 50, 5, true);

        mvc.perform(get("/api/v1/inventory/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(50))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void multipleProductAvailabilityWorks() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        service.bulkResponse = List.of(
                new InventoryAvailabilityResponse(first, 50, 5, true),
                new InventoryAvailabilityResponse(second, 0, 0, false)
        );

        mvc.perform(get("/api/v1/inventory/availability")
                        .param("productIds", first + "," + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(first.toString()))
                .andExpect(jsonPath("$[1].available").value(false));
    }

    @Test
    void reserveInventorySucceeds() throws Exception {
        String request = """
                {
                  "items": [
                    {
                      "productId": "10000000-0000-0000-0000-000000000001",
                      "quantity": 2
                    }
                  ]
                }
                """;

        mvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(service.reserveCalled).isTrue();
    }

    @Test
    void reserveInventoryFailsWhenStockIsInsufficient() throws Exception {
        service.reserveException = new InsufficientStockException(
                UUID.fromString("10000000-0000-0000-0000-000000000001")
        );
        String request = """
                {
                  "items": [
                    {
                      "productId": "10000000-0000-0000-0000-000000000001",
                      "quantity": 999
                    }
                  ]
                }
                """;

        mvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void releaseInventorySucceeds() throws Exception {
        String request = """
                {
                  "items": [
                    {
                      "productId": "10000000-0000-0000-0000-000000000001",
                      "quantity": 2
                    }
                  ]
                }
                """;

        mvc.perform(post("/api/v1/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(service.releaseCalled).isTrue();
    }

    @Test
    void inventoryNotFoundReturns404() throws Exception {
        UUID productId = UUID.randomUUID();
        service.singleException = new InventoryNotFoundException(productId);

        mvc.perform(get("/api/v1/inventory/{productId}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INVENTORY_NOT_FOUND"));
    }

    private static final class FakeInventoryService extends InventoryService {
        private InventoryAvailabilityResponse singleResponse;
        private List<InventoryAvailabilityResponse> bulkResponse = List.of();
        private RuntimeException singleException;
        private RuntimeException reserveException;
        private boolean reserveCalled;
        private boolean releaseCalled;

        private FakeInventoryService() {
            super(null, null);
        }

        @Override
        public InventoryAvailabilityResponse getAvailability(UUID productId) {
            if (singleException != null) {
                throw singleException;
            }
            return singleResponse;
        }

        @Override
        public List<InventoryAvailabilityResponse> getAvailability(List<UUID> productIds) {
            return bulkResponse;
        }

        @Override
        public void reserve(ReserveInventoryRequest request) {
            reserveCalled = true;
            if (reserveException != null) {
                throw reserveException;
            }
        }

        @Override
        public void release(ReleaseInventoryRequest request) {
            releaseCalled = true;
        }
    }
}
