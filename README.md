# Cloud-Native E-Commerce Platform

This repository contains a demo-friendly retail platform built with Java 21, Spring Boot 3, Maven, PostgreSQL, Flyway, Docker Compose, REST APIs, Kafka, Resilience4j, Micrometer, Prometheus, and Zipkin.

The platform keeps each service independently deployable with its own PostgreSQL database:

- Product Service owns catalog APIs.
- Inventory Service owns stock availability and reservations.
- Cart Service owns cart APIs.
- Order Service owns order placement, order history, transactional outbox publishing, and final saga status.
- Payment Service simulates payment processing.
- Shipping Service simulates shipment creation.

Virtual threads are enabled in every service with `spring.threads.virtual.enabled=true`.

## Architecture

```text
Client
  |
  v
Order Service
  |
  | saves order + outbox event
  v
order-db / outbox_events
  |
  v
Outbox Publisher
  |
  v
Kafka: order.events
  |
  v
Inventory Service
  |
  v
Kafka: inventory.events
  |
  v
Payment Service
  |
  v
Kafka: payment.events
  |
  v
Shipping Service
  |
  v
Kafka: shipping.events
  |
  v
Order Service updates final order status
```

Kafka topics:

- `order.events`
- `inventory.events`
- `payment.events`
- `shipping.events`
- `dead-letter.events`

Domain events use JSON with a common envelope:

```json
{
  "eventId": "uuid",
  "eventType": "OrderPlaced",
  "aggregateId": "orderId",
  "correlationId": "uuid",
  "occurredAt": "2026-06-29T10:15:30Z",
  "sourceService": "order-service",
  "payload": {}
}
```

`correlationId` is preserved across the order workflow so logs and traces can be followed end to end.

## Saga Flow

1. `POST /api/v1/orders` creates an order with `CREATED` status.
2. Order Service writes an `OrderPlaced` event to `outbox_events` in the same database transaction.
3. The scheduled outbox publisher publishes pending events to `order.events`.
4. Inventory Service consumes `OrderPlaced`.
5. Inventory reserves stock and publishes `InventoryReserved`, or publishes `InventoryReservationFailed`.
6. Payment Service consumes `InventoryReserved` and publishes `PaymentProcessed` or `PaymentFailed`.
7. Shipping Service consumes `PaymentProcessed` and publishes `OrderShipped`.
8. Order Service consumes inventory, payment, and shipping events and updates order status/history.

Order statuses:

- `CREATED`
- `INVENTORY_RESERVED`
- `INVENTORY_FAILED`
- `PAYMENT_PROCESSED`
- `PAYMENT_FAILED`
- `SHIPPED`
- `CONFIRMED`
- `CANCELLED`

The happy-path final status is `SHIPPED`.

## Transactional Outbox

Order Service implements the transactional outbox pattern:

- `OrderService` saves the order, order items, status history, and `outbox_events` row in one transaction.
- `OutboxPublisher` runs every few seconds.
- Pending and retryable failed rows are published to Kafka.
- Successful rows are marked `PUBLISHED`.
- Failed rows increment `retry_count`, store `last_error`, and remain `FAILED` after max retries.

This keeps order creation independent from Kafka availability and avoids publishing events before the order transaction commits.

## Idempotency, Retry, And Dead Letter Handling

Kafka consumers in Order, Inventory, Payment, and Shipping use a `processed_events` table:

- If `event_id` already exists, the event is skipped and logged as a duplicate.
- If it does not exist, the event is processed and recorded.

Consumer retry uses Spring Kafka error handling. After configured retry attempts, failed records are sent to `dead-letter.events`. Each service has a dead-letter listener that logs the failed event with event id, event type, aggregate id, and service context.

## Resilience

Resilience4j is configured for downstream REST calls:

- `inventoryServiceCircuitBreaker`
- `inventoryServiceRetry`
- `productServiceCircuitBreaker`
- `productServiceRetry`

The existing Order Service inventory REST client has circuit breaker, retry, and fallback behavior. The async Kafka flow is the primary order workflow, but the REST client remains protected for compatibility and future use.

## Observability

Every service exposes:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

Docker Compose includes:

- Kafka UI at `http://localhost:8090`
- Zipkin at `http://localhost:9411`
- Prometheus at `http://localhost:9090`

Micrometer tracing is enabled with full sampling for demos. Kafka producer and listener observation is enabled in Kafka-based services.

Business metrics include:

- `orders.created.count`
- `orders.confirmed.count`
- `orders.cancelled.count`
- `inventory.reserved.count`
- `payment.processed.count`
- `shipping.created.count`
- `kafka.events.published.count`
- `kafka.events.consumed.count`
- `kafka.events.failed.count`

Structured logs include fields such as `correlationId`, `eventId`, `orderId`, `service`, and `eventType`.

## Local Ports

| Service | Port | Swagger |
| --- | ---: | --- |
| Product Service | 8081 | `http://localhost:8081/swagger-ui.html` |
| Inventory Service | 8082 | `http://localhost:8082/swagger-ui.html` |
| Cart Service | 8083 | `http://localhost:8083/swagger-ui.html` |
| Order Service | 8084 | `http://localhost:8084/swagger-ui.html` |
| Payment Service | 8085 | `http://localhost:8085/swagger-ui.html` |
| Shipping Service | 8086 | `http://localhost:8086/swagger-ui.html` |

| Infrastructure | Port | URL |
| --- | ---: | --- |
| Kafka | 9092 | `localhost:9092` |
| Kafka UI | 8090 | `http://localhost:8090` |
| Zipkin | 9411 | `http://localhost:9411` |
| Prometheus | 9090 | `http://localhost:9090` |

| Database | Local Port |
| --- | ---: |
| product-db | 5433 |
| inventory-db | 5434 |
| cart-db | 5435 |
| order-db | 5436 |
| payment-db | 5437 |
| shipping-db | 5438 |

## Run With Docker Compose

```bash
docker compose up --build
```

Each service runs Flyway migrations on startup against its own PostgreSQL database. Kafka topics are created by the Spring Boot services.

## Demo Flow

Place an order:

```bash
curl -X POST http://localhost:8084/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "30000000-0000-0000-0000-000000000001",
    "items": [
      {
        "productId": "10000000-0000-0000-0000-000000000001",
        "productName": "iPhone 15",
        "unitPrice": 79999.00,
        "quantity": 2
      }
    ]
  }'
```

Expected immediate response:

- Order is created with `CREATED` status.
- Response includes `orderId`, `status`, `totalAmount`, and a workflow message.
- `OrderPlaced` is saved to `order-db.outbox_events`.

Check status:

```bash
curl http://localhost:8084/api/v1/orders/{orderId}/status
```

Check history:

```bash
curl http://localhost:8084/api/v1/orders/{orderId}/history
```

Expected happy-path history:

- `CREATED`
- `INVENTORY_RESERVED`
- `PAYMENT_PROCESSED`
- `SHIPPED`

Check payment and shipment:

```bash
curl http://localhost:8085/api/v1/payments/order/{orderId}
curl http://localhost:8086/api/v1/shipments/order/{orderId}
```

Open Kafka UI and confirm events in `order.events`, `inventory.events`, `payment.events`, and `shipping.events`.

Open Zipkin and search by service name, for example `order-service`, `inventory-service`, `payment-service`, or `shipping-service`.

## Failure Demos

Insufficient stock:

```bash
curl -X POST http://localhost:8084/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "30000000-0000-0000-0000-000000000001",
    "items": [
      {
        "productId": "10000000-0000-0000-0000-000000000001",
        "productName": "iPhone 15",
        "unitPrice": 79999.00,
        "quantity": 999999
      }
    ]
  }'
```

Inventory publishes `InventoryReservationFailed`, and Order Service moves the order to `CANCELLED`.

Payment failure:

```bash
curl -X POST http://localhost:8084/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "30000000-0000-0000-0000-000000000099",
    "items": [
      {
        "productId": "10000000-0000-0000-0000-000000000001",
        "productName": "iPhone 15",
        "unitPrice": 79999.00,
        "quantity": 1
      }
    ]
  }'
```

Payment Service publishes `PaymentFailed`, and Order Service moves the order to `CANCELLED`. The failure customer id can be changed with `PAYMENT_FAILURE_CUSTOMER_ID`.

Consumer failure:

- Publish an event with `"simulateConsumerFailure": true` in the payload to a consumed topic.
- The consumer throws, retries, then the configured error handler sends the record to `dead-letter.events`.

Duplicate event:

- Reprocess a Kafka message with the same `eventId`.
- The receiving service skips it because the id is already in `processed_events`.

## API Summary

Order Service:

- `POST /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `GET /api/v1/orders/{orderId}/status`
- `GET /api/v1/orders/{orderId}/history`

Inventory Service:

- `GET /api/v1/inventory/{productId}`
- `POST /api/v1/inventory/reserve`
- `POST /api/v1/inventory/release`

Payment Service:

- `GET /api/v1/payments/{paymentId}`
- `GET /api/v1/payments/order/{orderId}`

Shipping Service:

- `GET /api/v1/shipments/{shipmentId}`
- `GET /api/v1/shipments/order/{orderId}`

## Tests

```bash
cd product-service && mvn test
cd ../inventory-service && mvn test
cd ../cart-service && mvn test
cd ../order-service && mvn test
cd ../payment-service && mvn test
cd ../shipping-service && mvn test
```

## Documentation

- [Architecture](docs/architecture.md)
- [API Contracts](docs/api-contracts.md)
