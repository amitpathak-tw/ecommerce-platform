package com.example.ecommerce.cart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI cartApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cart Service API")
                        .version("v1")
                        .description("Cart creation, item management and summary APIs."));
    }
}
