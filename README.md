# Cloud-Native E-Commerce Platform

Four independently deployable Spring Boot services implement a basic e-commerce flow:

- Product Service owns catalog and category APIs.
- Inventory Service owns stock availability, reservation, and release.
- Cart Service owns cart creation and item management.
- Order Service owns order placement, order status, and order history.

## Technology

Java 21, Spring Boot 3.x, Spring Web, Spring Data JPA, PostgreSQL, Flyway, Docker, Docker Compose, OpenAPI, JUnit 5, AssertJ, Mockito where useful, and Maven.

Virtual threads are enabled in every service with `spring.threads.virtual.enabled=true`.

## Local Ports

| Service | Port | Swagger |
| --- | ---: | --- |
| Product Service | 8081 | `http://localhost:8081/swagger-ui.html` |
| Inventory Service | 8082 | `http://localhost:8082/swagger-ui.html` |
| Cart Service | 8083 | `http://localhost:8083/swagger-ui.html` |
| Order Service | 8084 | `http://localhost:8084/swagger-ui.html` |

| Database | Local Port |
| --- | ---: |
| product-db | 5433 |
| inventory-db | 5434 |
| cart-db | 5435 |
| order-db | 5436 |

## Run With Docker Compose

```bash
docker compose up --build
```

Each service runs Flyway migrations on startup against its own PostgreSQL database.

## Run A Service Locally

Start the needed PostgreSQL database, then run:

```bash
cd product-service
mvn spring-boot:run
```

Use the equivalent service directory for `inventory-service`, `cart-service`, or `order-service`.

## Tests

```bash
cd product-service && mvn test
cd ../inventory-service && mvn test
cd ../cart-service && mvn test
cd ../order-service && mvn test
```

## Sample Flow

Browse products:

```bash
curl http://localhost:8081/api/v1/products
```

Check inventory:

```bash
curl http://localhost:8082/api/v1/inventory/10000000-0000-0000-0000-000000000001
```

Create a cart:

```bash
curl -X POST http://localhost:8083/api/v1/carts \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"30000000-0000-0000-0000-000000000001"}'
```

Place an order:

```bash
curl -X POST http://localhost:8084/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId":"30000000-0000-0000-0000-000000000001",
    "items":[
      {
        "productId":"10000000-0000-0000-0000-000000000001",
        "productName":"iPhone 15",
        "unitPrice":79999.00,
        "quantity":2
      }
    ]
  }'
```

## Documentation

- [Architecture](docs/architecture.md)
- [API Contracts](docs/api-contracts.md)

## Future Improvements

API Gateway, JWT authentication, role-based access, service discovery, centralized logging, distributed tracing, Kafka-based async order flow, Redis cache, Elasticsearch product search, NoSQL recommendations or sessions, Payment Service, and Notification Service.
