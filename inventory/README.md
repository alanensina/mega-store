# Inventory Service

Inventory manages Mega Store's product catalogue and stock. It also consumes order events from Kafka and converts them into stock reservations, so reservation creation and changes are driven by the Order service rather than by a public reservation-write endpoint.

## Features

- Create, list, fetch, update, and delete products.
- Store product name, price, active status, and stock quantity; product creation initializes stock.
- Create, list, fetch, update, and delete stock records.
- Consume `CREATE`, `UPDATE`, and `CANCEL` order events to create or modify reservations and adjust stock.
- Reject reservations for inactive products or when available stock is insufficient.
- Deduplicate order events by event ID so Kafka redelivery does not apply the same stock change twice.
- Retry transient Kafka consumer failures with exponential backoff and publish exhausted messages to `<source-topic>.DLT`.
- Detect concurrent stock/reservation writes with optimistic locking and return `409 Conflict`.
- Upload product images to AWS S3 and store the resulting URL with the product.
- Apply schema changes automatically with Flyway.

## Technology and local dependencies

- Java 26, Spring Boot, Spring Data JPA, Spring Kafka, and Maven
- PostgreSQL at `localhost:5433`, database `inventory-service`
- Kafka at `localhost:9094`, consuming the `orders_topic` topic
- Environment variables: `INVENTORY_DB_USERNAME`, `INVENTORY_DB_PASSWORD`, `JWT_SECRET`, `AWS_REGION`, and `AWS_S3_BUCKET`. AWS credentials are resolved by the AWS SDK default credentials provider.

Start PostgreSQL and Kafka with Docker Compose from the repository root. See the [project README](../README.md) for complete setup details. Flyway applies migrations on startup.

## Run

From the repository root:

```bash
./inventory/mvnw -f inventory/pom.xml spring-boot:run
```

The API listens on `http://localhost:8080`.

## API

Base URL: `http://localhost:8080`
All `/api` endpoints require a Customer-issued JWT:

```http
Authorization: Bearer <jwt-token>
```

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/product` | Create a product and its initial stock |
| `GET` | `/api/product` | List products |
| `GET` | `/api/product/{id}` | Fetch a product |
| `PUT` | `/api/product` | Update a product; optional `price` and `quantity` fields can also update price and stock |
| `DELETE` | `/api/product/{id}` | Delete a product and its stock |
| `POST` | `/api/stock` | Create a stock record |
| `GET` | `/api/stock` | List stock records |
| `GET` | `/api/stock/{id}` | Fetch a stock record |
| `PUT` | `/api/stock` | Update a stock record |
| `DELETE` | `/api/stock/{id}` | Delete a stock record |
| `GET` | `/api/reservation` | List reservations |
| `GET` | `/api/reservation/{id}` | Fetch a reservation |

Reservations are created, updated, and cancelled from consumed Order events; this service does not expose reservation `POST`, `PUT`, or `DELETE` routes.

### Create a product

Product creation includes its initial price and inventory quantity. JSON is still accepted, and you can also send `multipart/form-data` with an `image` file to upload to S3.

```http
POST /api/product
Content-Type: application/json
Authorization: Bearer <jwt-token>
```

```json
{
  "name": "T-shirt",
  "price": 24.99,
  "active": true,
  "quantity": 100
}
```

Names are unique without regard to case. Price and quantity cannot be negative. When an image is provided, Inventory uploads it to S3 and stores the URL in `imageUrl`. When updating a product, `price` and `quantity` may be omitted to keep their current values.

The API accepts product creation as JSON without an image or as `multipart/form-data` with an `image` part. Uploaded files must have an `image/*` content type and are limited to 5 MB. The admin UI requires an image when creating a product. Configure the S3 bucket so the image URLs returned by Inventory can be fetched by browsers.

## Kafka order processing

Inventory consumes order events from `orders_topic`. Each event includes an event UUID, cart UUID, event type, items, and order status. Processed event IDs are stored so that a redelivery does not apply the same operation again. Legacy messages without an event ID receive a deterministic ID derived from their payload.

The consumer retries failures with exponential backoff. After retries are exhausted, the message is published to a dead-letter topic named after the source topic with the `.DLT` suffix (for example, `orders_topic.DLT`). Ensure the Kafka broker is reachable and that the consumer is permitted to create or publish to the dead-letter topic.

## Security and API docs

Inventory validates JWTs locally; it does not issue tokens. Obtain a JWT from Customer's `POST /api/auth` endpoint. The `JWT_SECRET` value must be identical in Customer, Inventory, and Order.

Swagger UI: [http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui)
OpenAPI document: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Use Swagger's **Authorize** control with the Customer-issued token to call protected routes.

## Build and test

From the repository root:

```bash
./inventory/mvnw -f inventory/pom.xml test
./inventory/mvnw -f inventory/pom.xml clean package
```

The tests cover product, stock, and reservation services; product and stock controller behavior; Kafka order-event processing; JWT security; and S3 configuration, upload validation, and error handling.
