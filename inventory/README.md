# Inventory Service (inventory)

A small Spring Boot microservice that manages products, stock and reservations. Built with Java, Spring Data JPA, Flyway and PostgreSQL. Includes REST APIs and OpenAPI/Swagger UI.

## Key features

- CRUD for Products and Stock
- Reservation creation and cancellation with stock checks
- Flyway migrations for schema (PRODUCT, STOCK, RESERVATION, RESERVATION_ITEM)
- OpenAPI / Swagger UI at /swagger-ui

## Tech stack

- Java 26
- Spring Boot 4.1.x
- Spring Data JPA
- Flyway for DB migrations
- PostgreSQL (runtime)
- Maven
- Lombok
- Spring Security with JWT authentication
- Auth0 Java JWT
- springdoc-openapi (Swagger UI)

## Project structure (important files)

- src/main/java/com/alanensina/inventory/
  - controllers: ProductController, StockController, ReservationController
  - services: ProductService, StockService, ReservationService
  - repositories: ProductRepository, StockRepository, ReservationRepository
  - dtos: CreateProductDTO, ProductDTO, UpdateProductDTO, CreateStockDTO, StockDTO, CreateReservationDTO, ReservationDTO, ReservationItemDTO
  - models: Product, Stock, Reservation, ReservationItem
  - exceptions: domain-specific exceptions
  - configs: SwaggerConfig
- src/main/resources/db/migration: Flyway SQL migration files V1..V3
- src/main/resources/application.properties
- pom.xml

## Database / Migrations

Flyway migrations are in src/main/resources/db/migration:
- V1__create_table_product.sql
- V2__create_table_stock.sql
- V3__create_table_reservation.sql

They create the PRODUCT, STOCK, RESERVATION and RESERVATION_ITEM tables and useful indexes.

## Configuration

Edit src/main/resources/application.properties or provide environment-specific overrides.
Important properties:

spring.datasource.url=jdbc:postgresql://localhost:5432/inventory-service
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.jpa.hibernate.ddl-auto=none

jwt.secret=changeit

Swagger/OpenAPI:
- Swagger UI: /swagger-ui
- OpenAPI JSON: /v3/api-docs

## Security

All Product, Stock, and Reservation API endpoints require a valid JWT issued by the Customer API `POST /api/auth` endpoint. The Inventory service validates the token locally using the same `jwt.secret`; it does not issue or accept a separate Inventory token.

Send the token in every protected request:

```text
Authorization: Bearer <jwt-token>
```

Swagger UI and the OpenAPI document are public. Click **Authorize** in Swagger UI and provide the JWT returned by the Customer API login before executing Product, Stock, or Reservation requests. Health and info actuator endpoints remain public.

## Build and run

Prerequisites: JDK 26, Maven, PostgreSQL running and reachable.

1. Build:

   mvn -f inventory/pom.xml clean package

2. Run (after DB configured and reachable):

   java -jar inventory/target/inventory-0.0.1-SNAPSHOT.jar

Or use your IDE to run com.alanensina.inventory.InventoryApplication.

## API Endpoints

Base path: /api

Products
- POST /api/product
  - Request: { "name": "Product name", "active": true }
  - Response: ProductDTO { id, name, active, createdAt, updatedAt }

- GET /api/product
  - Response: [ ProductDTO, ... ]

- GET /api/product/{id}
  - Response: ProductDTO

- PUT /api/product/{id}
  - Request: { "name": "Updated name", "active": false }
  - Response: updated ProductDTO

- DELETE /api/product/{id}
  - Response: deleted ProductDTO

Stock
- POST /api/stock
  - Request: { "productId": "<product-uuid>", "quantity": 10 }
  - Response: StockDTO { id, product, quantity }

- GET /api/stock
  - Response: [ StockDTO, ... ]

- GET /api/stock/{id}
  - Response: StockDTO

- PUT /api/stock/{id}
  - Request: { "productId": "<product-uuid>", "quantity": 42 }
  - Response: updated StockDTO

- DELETE /api/stock/{id}
  - Response: deleted StockDTO

Reservations
- POST /api/reservation
  - Request:
    {
      "cartId": "<cart-uuid>",
      "items": [ { "productId": "<product-uuid>", "quantity": 2 }, ... ]
    }
  - Response: ReservationDTO { id, cartId, items, status }

- PUT /api/reservation/cancel/{id}
  - Cancels a reservation and (depending on logic) restores stock.
  - Response: ReservationDTO (cancelled)

- GET /api/reservation
  - Response: [ ReservationDTO, ... ]

- GET /api/reservation/{id}
  - Response: ReservationDTO

## Example curl

Create a product:

curl -X POST http://localhost:8080/api/product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{"name":"T-shirt","active":true}'

Create stock:

curl -X POST http://localhost:8080/api/stock \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{"productId":"<product-uuid>","quantity":100}'

Create reservation:

curl -X POST http://localhost:8080/api/reservation \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{"cartId":"<cart-uuid>","items":[{"productId":"<product-uuid>","quantity":2}]}'