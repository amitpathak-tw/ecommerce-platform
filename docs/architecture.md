# Architecture

This project is implemented as six independently deployable Spring Boot microservices:

- Product Service owns product catalog and category data.
- Inventory Service owns stock availability, reservation, and release.
- Cart Service owns customer carts and cart items.
- Order Service owns order placement, order status, order history, and transactional outbox publishing.
- Payment Service owns simulated payment processing.
- Shipping Service owns simulated shipment creation.

Each service owns its own PostgreSQL database. Existing REST APIs remain available, and the order workflow now uses Kafka events for asynchronous saga coordination.

## Bounded Contexts

- Product catalog data is isolated in `product_db`.
- Inventory data is isolated in `inventory_db`.
- Cart data is isolated in `cart_db`.
- Order data, status history, outbox rows, and processed event ids are isolated in `order_db`.
- Payment data and processed event ids are isolated in `payment_db`.
- Shipment data and processed event ids are isolated in `shipping_db`.
- APIs return DTO records and do not expose JPA entities.

## Event-Driven Order Flow

1. Customer places an order through Order Service.
2. Order Service saves the order and an `OrderPlaced` outbox row in one transaction.
3. The scheduled outbox publisher sends `OrderPlaced` to `order.events`.
4. Inventory Service consumes `OrderPlaced` and publishes `InventoryReserved` or `InventoryReservationFailed`.
5. Payment Service consumes `InventoryReserved` and publishes `PaymentProcessed` or `PaymentFailed`.
6. Shipping Service consumes `PaymentProcessed` and publishes `OrderShipped`.
7. Order Service consumes inventory, payment, and shipping events and updates order status/history.

## Reliability

The platform uses:

- Transactional outbox in Order Service for reliable event publishing.
- `processed_events` tables for idempotent consumers.
- Spring Kafka retry and `dead-letter.events` for failed messages.
- Resilience4j circuit breaker and retry configuration for downstream REST calls.
- Micrometer, Prometheus, and Zipkin for metrics and tracing.

## Docker Compose

Docker Compose starts six PostgreSQL containers, Kafka, Zookeeper, Kafka UI, Zipkin, Prometheus, and all six Spring Boot services.
