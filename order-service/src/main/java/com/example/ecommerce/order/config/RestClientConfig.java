package com.example.ecommerce.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient inventoryRestClient(
            RestClient.Builder builder,
            @Value("${services.inventory.base-url}") String inventoryBaseUrl
    ) {
        return builder.baseUrl(inventoryBaseUrl).build();
    }
}
