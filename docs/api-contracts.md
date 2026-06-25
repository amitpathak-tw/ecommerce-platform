# API Contracts

## Error Response

All services use this response shape:

```json
{
  "timestamp": "2026-06-24T10:00:00Z",
  "status": 404,
  "errorCode": "PRODUCT_NOT_FOUND",
  "message": "Product not found: uuid",
  "path": "/api/v1/products/uuid",
  "validationErrors": {}
}
```

## Product Service

Base URL: `http://localhost:8081`

### Get Products

`GET /api/v1/products`

Query parameters:

- `categoryId`: optional UUID
- `search`: optional string
- `page`: optional int, default `0`
- `size`: optional int, default `20`
- `sort`: optional string, default `name`

Response:

```json
{
  "content": [
    {
      "id": "10000000-0000-0000-0000-000000000001",
      "name": "iPhone 15",
      "sku": "IPHONE-15",
      "price": 79999.00,
      "categoryId": "00000000-0000-0000-0000-000000000001",
      "categoryName": "Mobiles",
      "active": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Search Products

`GET /api/v1/products/search?query=iphone&page=0&size=20`

Uses the same search logic as `GET /api/v1/products`.

### Get Product By ID

`GET /api/v1/products/{productId}`

Response:

```json
{
  "id": "10000000-0000-0000-0000-000000000001",
  "name": "iPhone 15",
  "description": "Apple smartphone",
  "sku": "IPHONE-15",
  "price": 79999.00,
  "categoryId": "00000000-0000-0000-0000-000000000001",
  "categoryName": "Mobiles",
  "active": true,
  "createdAt": "2026-06-24T10:00:00Z",
  "updatedAt": "2026-06-24T10:00:00Z"
}
```

### Get Categories

`GET /api/v1/categories`

Response:

```json
[
  {
    "id": "00000000-0000-0000-0000-000000000001",
    "name": "Mobiles",
    "description": "Mobile phones and accessories"
  }
]
```

## Inventory Service

Base URL: `http://localhost:8082`

- `GET /api/v1/inventory/{productId}` returns one product availability.
- `GET /api/v1/inventory/availability?productIds=uuid1,uuid2` returns availability for multiple products.
- `POST /api/v1/inventory/reserve` reserves stock and returns `204 No Content`.
- `POST /api/v1/inventory/release` releases reserved stock and returns `204 No Content`.

Reserve/release request:

```json
{
  "items": [
    {
      "productId": "10000000-0000-0000-0000-000000000001",
      "quantity": 2
    }
  ]
}
```

Availability response:

```json
{
  "productId": "10000000-0000-0000-0000-000000000001",
  "availableQuantity": 50,
  "reservedQuantity": 0,
  "available": true
}
```

## Cart Service

Base URL: `http://localhost:8083`

- `POST /api/v1/carts` creates a cart.
- `GET /api/v1/carts/{cartId}` returns a cart.
- `POST /api/v1/carts/{cartId}/items` adds an item or increases quantity.
- `PUT /api/v1/carts/{cartId}/items/{itemId}` updates quantity.
- `DELETE /api/v1/carts/{cartId}/items/{itemId}` removes an item.
- `GET /api/v1/carts/{cartId}/summary` returns cart totals.

Add item request:

```json
{
  "productId": "10000000-0000-0000-0000-000000000001",
  "productName": "iPhone 15",
  "unitPrice": 79999.00,
  "quantity": 2
}
```

Cart response:

```json
{
  "id": "uuid",
  "customerId": "uuid",
  "status": "ACTIVE",
  "items": [
    {
      "id": "uuid",
      "productId": "10000000-0000-0000-0000-000000000001",
      "productName": "iPhone 15",
      "unitPrice": 79999.00,
      "quantity": 2,
      "totalPrice": 159998.00
    }
  ],
  "totalAmount": 159998.00
}
```

## Order Service

Base URL: `http://localhost:8084`

- `POST /api/v1/orders` reserves inventory and places an order.
- `GET /api/v1/orders/{orderId}` returns order details.
- `GET /api/v1/orders/{orderId}/status` returns current status.
- `GET /api/v1/orders/customer/{customerId}` returns customer orders.
- `GET /api/v1/orders/{orderId}/history` returns status history.

Place order request:

```json
{
  "customerId": "30000000-0000-0000-0000-000000000001",
  "items": [
    {
      "productId": "10000000-0000-0000-0000-000000000001",
      "productName": "iPhone 15",
      "unitPrice": 79999.00,
      "quantity": 2
    }
  ]
}
```

Order response:

```json
{
  "id": "uuid",
  "customerId": "30000000-0000-0000-0000-000000000001",
  "totalAmount": 159998.00,
  "status": "CONFIRMED",
  "items": [
    {
      "productId": "10000000-0000-0000-0000-000000000001",
      "productName": "iPhone 15",
      "unitPrice": 79999.00,
      "quantity": 2,
      "totalPrice": 159998.00
    }
  ],
  "createdAt": "2026-06-24T10:00:00Z"
}
```
