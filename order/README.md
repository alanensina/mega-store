# Order Service

Order provides the cart and order API for Mega Store. It persists each cart and its items in PostgreSQL, assigns a sequential order number, and publishes order and notification events to Kafka through a transactional outbox.

## Features

- Create, list, fetch, update, and cancel carts/orders.
- Calculate and persist order totals and expose a sequential `orderNumber` alongside each order UUID.
- Fetch customer name and email from Customer for notifications, forwarding the caller's JWT.
- Save Kafka event payloads in an outbox within the same database transaction as the order change.
- Publish pending outbox events on a schedule, with configurable exponential retry delays and a maximum attempt count.
- Initialize the configured Kafka topics on service startup and track initialization in the database.
- Use optimistic locking to detect conflicting order updates and return `409 Conflict`.
- Apply database migrations automatically through Flyway.

## Technology and local dependencies

- Java 26, Spring Boot, Spring Data JPA, Spring Kafka, and Maven
- PostgreSQL at `localhost:5435`, database `order-service`
- Kafka at `localhost:9094`
- Customer API at `http://localhost:8081` for customer details
- Environment variables: `ORDER_DB_USERNAME`, `ORDER_DB_PASSWORD`, and `JWT_SECRET`

The service publishes to `orders_topic` for Inventory and `orders_notification` for Notification. Start PostgreSQL and Kafka with Docker Compose, and start Customer before creating or cancelling orders. See the [project README](../README.md) for setup instructions.

## Run

From the repository root:

```bash
./order/mvnw -f order/pom.xml spring-boot:run
```

The API listens on `http://localhost:8082`.

## API

Base path: `/api/cart`
All cart endpoints require a JWT issued by Customer:

```http
Authorization: Bearer <jwt-token>
```

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/cart` | Create an order |
| `GET` | `/api/cart` | List orders |
| `GET` | `/api/cart/{id}` | Fetch an order by UUID |
| `PUT` | `/api/cart` | Update an order |
| `PUT` | `/api/cart/cancel/{id}` | Cancel an order |

Create request example:

```json
{
  "customerId": "customer-uuid",
  "items": [
    {
      "productId": "product-uuid",
      "quantity": 2,
      "price": 24.99
    }
  ]
}
```

The response includes the order UUID, sequential `orderNumber`, customer UUID, total price, status, timestamps, and item list. The service records an order event for Inventory and a notification event for Notification when an order is created. Cancellation records corresponding cancel events. Updates publish an order event for Inventory.

Swagger UI: [http://localhost:8082/swagger-ui](http://localhost:8082/swagger-ui)
OpenAPI document: [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs)

## Transactional outbox and delivery

Order changes and their Kafka events are stored in the same PostgreSQL transaction. A scheduled publisher reads pending events in batches and sends them to their configured topic. This avoids losing an event when an order is committed but Kafka is temporarily unavailable.

Publishing failures increment the event's attempt count and schedule a later retry with capped exponential backoff. Defaults are a one-second initial delay, a five-minute maximum delay, and ten attempts; these can be overridden with:

| Property | Default | Purpose |
| --- | ---: | --- |
| `order.outbox.publish-delay-ms` | `1000` | Delay between publish scans |
| `order.outbox.retry-initial-delay-ms` | `1000` | Initial retry delay |
| `order.outbox.retry-max-delay-ms` | `300000` | Maximum retry delay |
| `order.outbox.max-attempts` | `10` | Attempts before an event is held for inspection |

Events that reach the attempt limit remain unpublished in the outbox for investigation; they are not silently discarded.

## Security and configuration

Order validates Customer-issued JWTs locally. Set `JWT_SECRET` to the same value used by Customer and Inventory. When Order calls `GET /api/customer/{id}`, it forwards the original request's `Authorization` header.

Database credentials are configured with `ORDER_DB_USERNAME` and `ORDER_DB_PASSWORD`. Kafka defaults to `localhost:9094`; topic names can be configured with `spring.kafka.orders.topic.name` and `spring.kafka.orders.notification.topic.name`.

## Build and test

From the repository root:

```bash
./order/mvnw -f order/pom.xml test
./order/mvnw -f order/pom.xml clean package
```
