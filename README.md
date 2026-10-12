# Mega Store

The main idea of this project is to create a superstore and use it as a study base to learn new technologies and how to apply them to real-world problems encountered in day-to-day work.

Mega Store is a local e-commerce application built from four Spring Boot microservices and an Angular storefront. The services communicate through REST APIs and Kafka, and each service persists its own data in PostgreSQL.

## Platform at a glance

| Component | Port | Responsibility |
| --- | ---: | --- |
| Customer | `8081` | Customer registration, profiles, login, and JWT issuance |
| Inventory | `8080` | Products, stock, and order-driven reservations |
| Order | `8082` | Cart/order lifecycle and order/notification events |
| Notification | `8083` | Kafka-driven customer email delivery |
| UI | `4200` | Storefront and admin interface |

Each backend has an independent PostgreSQL database. Kafka runs locally on `localhost:9094`.

## How an order flows through the system

1. A customer signs in through Customer and receives a JWT. The UI sends it to protected APIs as a Bearer token.
2. The UI submits a cart to Order. Order fetches customer details from Customer, forwarding the caller's JWT.
3. In one database transaction, Order saves the cart and corresponding outbox events. A scheduled publisher sends pending events to Kafka, retrying failures with backoff.
4. Inventory consumes order events to create, update, or cancel stock reservations. It records processed event IDs to avoid applying duplicate deliveries twice. Transient consumer failures are retried; exhausted records are routed to a dead-letter topic.
5. Notification consumes order email events and uses SMTP to send new-order or cancellation messages. Delivery records and temporary leases help suppress duplicate processing and coordinate retries.

## Services

| Service | Database | Kafka topics | API documentation |
| --- | --- | --- | --- |
| [Customer](customer/README.md) | `localhost:5434/customer-service` | — | [Swagger](http://localhost:8081/swagger-ui) |
| [Inventory](inventory/README.md) | `localhost:5433/inventory-service` | Consumes `orders_topic` | [Swagger](http://localhost:8080/swagger-ui) |
| [Order](order/README.md) | `localhost:5435/order-service` | Publishes `orders_topic`, `orders_notification` | [Swagger](http://localhost:8082/swagger-ui) |
| [Notification](notification/README.md) | `localhost:5436/notification-service` | Consumes `orders_notification` | No public CRUD API |

The UI has its own [README](ui/README.md), including its local development server and API proxy configuration.

## Requirements

- JDK 26
- Docker Compose for local PostgreSQL and Kafka
- Node.js compatible with Angular 22 and npm to run the UI
- A Gmail account with an App Password, or another compatible SMTP setup, to deliver emails

## Run locally

### 1. Configure environment variables

Create a `.env` file in the repository root. Compose reads it to configure PostgreSQL; the same values must be available in the shell that starts the Java services.

```dotenv
INVENTORY_DB_USERNAME=inventory
INVENTORY_DB_PASSWORD=change-me
CUSTOMER_DB_USERNAME=customer
CUSTOMER_DB_PASSWORD=change-me
ORDER_DB_USERNAME=orders
ORDER_DB_PASSWORD=change-me
NOTIFICATION_DB_USERNAME=notification
NOTIFICATION_DB_PASSWORD=change-me
JWT_SECRET=replace-with-a-long-random-secret-shared-by-customer-inventory-and-order
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-public-products-bucket
MAIL_USERNAME=your-account@gmail.com
MAIL_PASSWORD=your-gmail-app-password
```

Keep real credentials private and do not commit `.env`. Docker Compose reads this file automatically. In every terminal that starts a Java service, export its variables before launching it:

```bash
set -a
source .env
set +a
```

### 2. Start PostgreSQL and Kafka

From the repository root:

```bash
docker compose up -d
```

This starts the four databases on ports `5433`–`5436` and Kafka on `9094`. It does not start the Java services or the UI.

### 3. Start the backend services

Run each command in a separate terminal from the repository root. Database migrations are applied by Flyway when each service starts.

```bash
./customer/mvnw -f customer/pom.xml spring-boot:run
./inventory/mvnw -f inventory/pom.xml spring-boot:run
./order/mvnw -f order/pom.xml spring-boot:run
./notification/mvnw -f notification/pom.xml spring-boot:run
```

Order initializes its configured Kafka topics on startup. Notification email delivery requires valid SMTP credentials; without them, other services can still run, but emails will not be delivered.

### 4. Start the UI

In another terminal:

```bash
cd ui
npm ci
npm start
```

Open [http://localhost:4200](http://localhost:4200). The Angular development server proxies `/api/product` and `/api/stock` to Inventory, `/api/cart` to Order, and other `/api` calls to Customer.

## Authentication and API docs

Customer issues signed JWTs from `POST /api/auth`. Inventory and Order validate those tokens locally, so `JWT_SECRET` must be identical for all three services. Customer registration and login are public; protected endpoints require:

```http
Authorization: Bearer <jwt-token>
```

Swagger UI is available at the links in the services table. Use its **Authorize** button with the JWT to try protected endpoints. Health and info actuator endpoints are exposed by the services.

## Build and test

Each service is an independent Maven project. For example, from the repository root:

```bash
./inventory/mvnw -f inventory/pom.xml clean package
./inventory/mvnw -f inventory/pom.xml test
```

Run the corresponding wrapper under `customer/`, `order/`, or `notification/` to build or test those services. For the UI, run `npm run build` or `npm test` from `ui/`.

## Repository layout

```text
customer/      Customer, profile, and authentication API
inventory/     Product, stock, and reservation API/consumer
order/         Cart/order API and Kafka outbox publisher
notification/  Kafka consumer and email delivery
ui/            Angular storefront and administration interface
docker-compose.yml
```
