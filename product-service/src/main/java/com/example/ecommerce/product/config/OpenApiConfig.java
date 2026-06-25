package com.example.ecommerce.product.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;
@Configuration public class OpenApiConfig { @Bean OpenAPI productApi(){return new OpenAPI().info(new Info().title("Product Service API").version("v1").description("Product catalog, browsing and category APIs."));} }
