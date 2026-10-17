# Notification Service

This service is part of the `mega-store` e-commerce platform and is responsible for sending e-mail notifications to customers based on order events produced by the order service.

It is an event-driven Spring Boot microservice that listens to Kafka topics, receives order-related payloads, and sends transactional emails for actions such as a new order placed and an order cancellation.

## How the application works

The flow is simple but effective:

1. The order service creates or cancels a cart/order.
2. It publishes a notification event to Kafka.
3. This service consumes the message from the configured Kafka topic.
4. The message is deserialized into a `NotificationDTO`.
5. Based on the event type, the service calls the correct e-mail method.
6. The e-mail is delivered through the configured SMTP server.

The main consumer is `OrderConsumer`, which listens to the topic configured in `application.properties` and handles these event types:

- `NEW_ORDER`: sends a confirmation e-mail
- `CANCEL`: sends a cancellation e-mail

The e-mail content is composed by `EmailService`, which uses Spring Mail to send plain-text messages.
When SMTP delivery fails, the service retries sends with exponential backoff before surfacing an error.

## Main responsibilities

- Consume Kafka notification events
- Validate supported event types
- Build order and cancellation messages
- Send e-mails via Gmail SMTP
- Log processing failures and unsupported events
- Retry transient SMTP failures with bounded exponential backoff

## Tech stack

- Java 26
- Spring Boot 4.1.1
- Spring Web
- Spring Kafka
- Spring Mail
- Maven
- Lombok
- Apache Kafka
- Gmail SMTP

## Unit test coverage

The Notification service has 100% unit test coverage for lines, methods and branches.

## Project structure

- `src/main/java/com/alanensina/notification/`
  - `kafka/`: Kafka listeners and event consumption logic
  - `services/`: e-mail sending logic
  - `dtos/`: message payloads
  - `enums/`: notification status and event definitions
  - `exceptions/`: custom exceptions for invalid event types and processing errors
  - `configurations/`: Kafka consumer configuration
- `src/main/resources/application.properties`: service configuration
- `pom.xml`: Maven dependencies and build configuration

## Event payload

The notification payload contains the customer name, e-mail, order/cart data, and the notification type.

Example data included in the event:

- customer name
- customer email
- order ID
- cart details
- total price
- status
- items
- notification type

This payload is transported as JSON over Kafka and deserialized automatically by Spring Kafka.

## Requirements

Before running the service, make sure you have:

- JDK 26 or compatible version
- Maven
- A running Kafka broker on `localhost:9094`
- A valid Gmail account with an App Password configured for SMTP access
- Access to the repository's Docker setup, if you want to start Kafka locally with Docker Compose

## Configuration

The main configuration is in `src/main/resources/application.properties`.

Key values include:

```properties
server.port=8083

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_account@gmail.com
spring.mail.password=your_app_password

spring.kafka.bootstrap-servers=localhost:9094
spring.kafka.consumer.group-id=notification
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.topic.name=orders_notification

notification.email.retry.max-attempts=3
notification.email.retry.initial-delay-ms=1000
notification.email.retry.max-delay-ms=5000
```

Important:

- `spring.mail.password` must be a valid Gmail App Password, not your normal account password.
- Kafka must be running before the notification service starts.
- The topic `orders_notification` must exist or be created automatically by the producer side of the system.

## Running the application

From the `notification` folder, run:

```bash
./mvnw spring-boot:run
```

The service will start on:

- HTTP: `http://localhost:8083`

This project does not expose a REST CRUD API. Its main function is to consume Kafka events and send emails asynchronously.

## Security

The Notification service does not expose client-facing REST endpoints and therefore does not participate in the JWT authentication flow used by Customer, Order, and Inventory. It consumes notification events from Kafka and should be protected through the broker's network and authentication configuration in deployment environments.

## Example flow

1. A customer creates an order through the order service.
2. The order service emits a `NEW_ORDER` notification event.
3. The notification service receives the message.
4. It sends an e-mail similar to:
   - "New order placed: ID..."
   - "Your order has been placed!"
5. If the order is cancelled, it sends another message with the cancellation information.

## Notes

- The service is intended to be used together with the other microservices in the `mega-store` project.
- It is asynchronous and reacts to events instead of receiving direct business requests from clients.
- Errors in mail delivery or unsupported Kafka event types are handled by custom exceptions and logged for troubleshooting.

## Docker / local infrastructure

The repository contains a `docker-compose.yml` file that starts the Kafka broker and related infrastructure used by the services.

To start Docker locally:

```bash
docker compose up -d
```

This service expects Kafka to be reachable at `localhost:9094`.
