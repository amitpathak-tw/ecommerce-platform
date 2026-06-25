# Architecture

This project is implemented as four independently deployable Spring Boot microservices:

- Product Service owns product catalog and category data.
- Inventory Service owns stock availability, reservation, and release.
- Cart Service owns customer carts and cart items.
- Order Service owns order placement, order status, and order history.

Each service owns its own PostgreSQL database. Services communicate synchronously over REST.

## Bounded Contexts

- Product catalog data is isolated in `product_db`.
- Inventory data is isolated in `inventory_db`.
- Cart data is isolated in `cart_db`.
- Order data is isolated in `order_db`.
- APIs return DTO records and do not expose JPA entities.

## Order Placement Flow

1. Customer browses products through Product Service.
2. Customer checks inventory through Inventory Service.
3. Customer creates a cart and adds items through Cart Service.
4. Customer places an order through Order Service.
5. Order Service reserves stock through Inventory Service before confirming the order.
6. Customer checks order status and order history through Order Service.

## Database Per Service

All schema changes are managed through Flyway migrations in each service. Docker Compose starts four PostgreSQL containers and passes database URLs to the matching service through environment variables.
