Order Service (order)
======================

Overview
--------
This is a Spring Boot microservice that implements an Order/Cart API. It stores carts and their items in PostgreSQL, publishes order and notification events to Kafka topics, and uses Flyway for schema migrations. The service also fetches customer details from an external Customer API to enrich notifications.

Main features
-------------
- REST API for creating, listing, retrieving, updating, and cancelling carts
- Persists carts and items in PostgreSQL (Flyway migrations included)
- Publishes OrderEvent and Notification messages to configurable Kafka topics
- Automatic topic initialization helper script (create_topics.sh)
- OpenAPI/Swagger UI available for interactive API exploration

API endpoints
-------------
Base path: /api/cart
- POST /api/cart        -> Create a new cart
- GET  /api/cart        -> List all carts
- GET  /api/cart/{id}   -> Get cart by id (UUID)
- PUT  /api/cart        -> Update cart
- PUT  /api/cart/cancel/{id} -> Cancel cart

Swagger UI: /swagger-ui
OpenAPI JSON: /v3/api-docs

Requirements
------------
- Java 26 (configured in pom.xml via <java.version>)
- Maven 3.6+
- PostgreSQL (default: jdbc:postgresql://localhost:5435/order-service)
- Kafka broker accessible (default: localhost:9094)
- Optional: Docker if using the provided topic creation script with a containerized Kafka

Configuration
-------------
Configuration keys are in src/main/resources/application.properties (overridable via environment variables or external properties):
- server.port (default 8082)
- spring.datasource.url / username / password
- spring.flyway.locations (migrations are in classpath:db/migration)
- spring.kafka.producer.bootstrap-servers / spring.kafka.bootstrap-servers
- spring.kafka.orders.topic.name (default orders_topic)
- spring.kafka.orders.notification.topic.name (default orders_notification)

Important: The service expects a Customer API at http://localhost:8081/api/customer/{id} to fetch customer name/email used in notifications. If unavailable, customer fetch errors will be raised.

Database migrations
-------------------
Flyway migrations are included under src/main/resources/db/migration (V1__create_order_table.sql and V2__create_topic_initialization_table.sql). Flyway runs on startup by default.

Kafka topics
------------
A helper script create_topics.sh is provided in resources to create topics if the kafka CLI or docker is available. The application also runs a small initializer (KafkaTopicInitializerConfig) that executes the script on startup and records initialization in a topic_initialization table.

Build & run
-----------
- Build: mvn clean package
- Run with Maven: mvn spring-boot:run
- Run jar: java -jar target/order-0.0.1-SNAPSHOT.jar

Testing
-------
- Run unit tests: mvn test

Notes
-----
- The project uses Lombok; IDEs should enable annotation processing.
- Adjust application.properties for production credentials, ports, and broker addresses.

License & authors
-----------------
See repository metadata. (No license file included in this module.)
