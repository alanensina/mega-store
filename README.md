# mega-store (microservices)

This repository contains a small set of Spring Boot microservices that make up a simple e-commerce platform (mega-store). Each service is a standalone Maven-based Java application. They communicate via REST and Kafka and persist data in PostgreSQL databases.

## Unit test coverage

All four microservices (Customer, Inventory, Order and Notification) have unit test coverage of 100% for lines, methods and branches.

Services
--------
- customer (port 8081)
  - Manages customer records and authentication (BCrypt).
  - PostgreSQL: jdbc:postgresql://localhost:5434/customer-service
  - Swagger UI: http://localhost:8081/swagger-ui
  - Endpoints: /api/customer, /api/auth

- inventory (port 8080)
  - Manages products, stock and reservations.
  - PostgreSQL: jdbc:postgresql://localhost:5433/inventory-service
  - Exposes product/stock/reservation CRUD and reservation logic.
  - Swagger UI: http://localhost:8080/swagger-ui

- order (port 8082)
  - Cart / order API, persists carts, publishes Order and Notification events to Kafka.
  - PostgreSQL: jdbc:postgresql://localhost:5435/order-service
  - Publishes to Kafka topics: orders_topic and orders_notification
  - Includes helper to initialize Kafka topics (create_topics.sh)
  - Swagger UI: http://localhost:8082/swagger-ui

- notification (port 8083)
  - Consumes Kafka notification events and sends transactional emails (Gmail SMTP).
  - Listens on Kafka topic: orders_notification
  - Does not expose a CRUD API — it reacts to events.

How they interact
-----------------
1. Clients authenticate through `POST /api/auth` on the Customer service and receive a signed JWT.
2. Clients send that JWT as `Authorization: Bearer <jwt-token>` when calling the protected Order and Inventory APIs.
3. The Order service publishes events to Kafka (orders_topic / orders_notification).
4. Inventory service can consume product/reservation events or be called directly to reserve stock.
5. Notification service consumes notification events and sends emails to customers.
6. Customer service stores customer data; Order service may call it to enrich notifications.

Spring Security
---------------
Customer, Order, and Inventory use Spring Security with JWT authentication. Customer is the token issuer; Order and Inventory validate the same token locally using the shared `jwt.secret`. Product, Stock, Reservation, and Cart endpoints require that token, while Customer login/registration, Swagger UI/OpenAPI, and health/info actuator endpoints are public as configured by each service.

The Order service propagates the original Bearer token when `CartService` calls the protected Customer endpoint to retrieve customer details. This preserves a single authentication flow and avoids creating a separate service token.

Each protected service exposes a `bearerAuth` scheme in Swagger UI. Use **Authorize** with the token returned by Customer `/api/auth` before making protected requests.

Requirements
------------
- JDK 26
- Maven (the projects include the Maven wrapper: ./mvnw)
- PostgreSQL (one database per service; see each service properties for ports)
- Kafka broker (default expected at localhost:9094)
- Docker & docker-compose (optional, a docker-compose.yml is included to start Kafka and related infra)
- For notification service emailing: a Gmail account with an App Password (for SMTP)

Local setup (quick)
-------------------
1. Create a local `.env` from `.env.example`, replace every placeholder with
   private values, and load it into the current shell:
   cp .env.example .env
   set -a; source .env; set +a

2. Start infrastructure with Docker Compose (optional):
   docker compose up -d
   This will start Kafka and support services configured in docker-compose.yml.

3. Ensure PostgreSQL instances/databases are available on the ports configured in each service's application.properties (inventory:5433, customer:5434, order:5435).

4. Build and run services (examples):
   - Build all modules (from repo root):
     mvn -T 1C -pl customer,inventory,order,notification -am clean package

   - Run a single service using the Maven wrapper:
     ./customer/mvnw -f customer/pom.xml spring-boot:run
     ./inventory/mvnw -f inventory/pom.xml spring-boot:run
     ./order/mvnw -f order/pom.xml spring-boot:run
     ./notification/mvnw -f notification/pom.xml spring-boot:run

   - Or run the packaged jars:
     java -jar inventory/target/inventory-0.0.1-SNAPSHOT.jar

5. Initialize Kafka topics (if needed):
   The order service contains a helper script (resources/create_topics.sh). Use it with a Kafka CLI or the containerized Kafka that comes from docker-compose.

Configuration
-------------
Each service exposes configuration in src/main/resources/application.properties. Important values to review and adapt before running:
- server.port (each service's HTTP port)
- spring.datasource.url / username / password (database connection)
- Kafka bootstrap servers and topic names (default: localhost:9094)
- For notification: spring.mail.* (SMTP host, username and App Password)
- JWT: `jwt.secret` must be the same in customer, inventory, and order

Useful endpoints
----------------
- customer Swagger: http://localhost:8081/swagger-ui
- inventory Swagger: http://localhost:8080/swagger-ui
- order Swagger: http://localhost:8082/swagger-ui

Notes & recommendations
-----------------------
- Flyway migrations run on startup; ensure DB users can create schema or pre-create DBs.
- Use the provided Maven wrapper (./mvnw) to avoid local Maven version issues.
- Set trusted packages or JSON deserializers appropriately for Kafka messages when exposing services across networks.
- Keep credentials out of source control; use environment variables or externalized configuration for production.

Where to find more
------------------
Each service has its own README in its folder with detailed API examples, DTOs, and instructions. See:
- customer/README.md
- inventory/README.md
- order/README.md
- notification/README.md
