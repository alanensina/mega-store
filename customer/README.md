# Customer API

Customer API is a Spring Boot REST service for managing customer records and authenticating customers by username and password. The service stores customer details in PostgreSQL, hashes passwords with BCrypt, and exposes Swagger UI documentation for testing the endpoints.

## Features

- Create, list, fetch, update, and delete customer records
- Prevent duplicate usernames or emails
- Validate input fields with Bean Validation
- Hash passwords with BCrypt before storing them
- Persist data with PostgreSQL and Flyway migrations
- Expose API documentation with SpringDoc OpenAPI

## Tech stack

- Java 26
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security Crypto (BCrypt)
- Spring Security with JWT authentication
- SpringDoc OpenAPI / Swagger UI
- Maven

## Database model

The application persists customer data in a `CUSTOMER` table with these fields:

- `id`: UUID primary key
- `name`: customer name
- `email`: customer email
- `username`: unique username
- `password`: BCrypt-hashed password
- `created_at`: creation timestamp
- `updated_at`: last update timestamp

The Flyway migration creates the table and adds unique/lookup indexes for email, username, and searchable fields.

## Configuration

The application configuration is in `src/main/resources/application.properties`.

```properties
server.port=8081

spring.datasource.url=jdbc:postgresql://localhost:5434/customer-service
spring.datasource.username=user
spring.datasource.password=your_password

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.jpa.hibernate.ddl-auto=none

springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.path=/swagger-ui
springdoc.api-docs.path=/v3/api-docs

jwt.secret=changeit
jwt.expiration-ms=3600000
```

Notes:

- The project expects PostgreSQL running locally on port `5434`.
- Flyway applies the migration automatically when the application starts.

## Security

Customer authentication is implemented with Spring Security and signed JWTs:

- `POST /api/auth` is public and authenticates a customer by username and password.
- A successful login returns an `AuthenticationResponse` containing the customer data and a JWT.
- `POST /api/customer` is public for customer registration.
- The remaining customer endpoints require `Authorization: Bearer <token>`.
- Tokens are signed with `jwt.secret` and expire according to `jwt.expiration-ms` (one hour by default).
- Invalid, expired, or missing tokens are rejected by the JWT authentication filter.

Swagger UI and the OpenAPI document are public so the API can be explored. Use the **Authorize** button in Swagger UI and enter the JWT returned by `/api/auth` as a Bearer token before calling protected endpoints.

## Running the application

Requirements:

- Java 26
- Maven wrapper compatible with the project
- PostgreSQL instance running and configured as above

Run the app with:

```bash
./mvnw spring-boot:run
```

The API will be available at:

- HTTP: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## API endpoints

### Authentication

#### POST `/api/auth`

Authenticates a customer by username and password.

Request body:

```json
{
  "username": "john",
  "password": "secret123"
}
```

Successful response:

```json
{
  "customer": {
    "id": "c7d0d8a3-4d3e-4f8b-9a2a-f73d1c4f0f21",
    "name": "John Doe",
    "email": "john@example.com",
    "username": "john",
    "createdAt": "2026-09-10T12:00:00",
    "updatedAt": "2026-09-10T12:00:00"
  },
  "token": "<jwt-token>"
}
```

### Customer management

#### POST `/api/customer`

Creates a new customer.

Request body:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "username": "john",
  "password": "secret123"
}
```

Validation rules:

- `name` is required
- `email` must be a valid email address
- `username` is required
- `password` must be between 8 and 16 characters

#### GET `/api/customer`

Returns all customers.

#### GET `/api/customer/{id}`

Returns a single customer by UUID.

#### PUT `/api/customer`

Updates an existing customer.

Request body:

```json
{
  "id": "c7d0d8a3-4d3e-4f8b-9a2a-f73d1c4f0f21",
  "name": "John Smith",
  "email": "johnsmith@example.com",
  "username": "johnsmith",
  "password": "newsecret123"
}
```

#### DELETE `/api/customer?id={id}`

Deletes a customer by ID.

## Business rules and validation

- Username and email must be unique across the system
- Authentication fails if the username is not found or the password does not match
- Passwords are never returned in API responses
- The service uses `CustomerDTO` to expose only safe customer fields

## Error handling

This API uses standard Spring Boot HTTP status responses:

- `400 Bad Request`: invalid credentials, missing customer, or validation failures
- `409 Conflict`: customer with the same username or email already exists
- `500 Internal Server Error`: unexpected server-side issue during password hashing or other processing

## Notes

- Passwords are stored as BCrypt hashes, not plain text.
- The `Customer` entity uses `UUID` as the identifier.
- Swagger is configured in `SwaggerConfig` and describes the project as `customer-api` with version `1.0`.

## Example cURL requests

Create customer:

```bash
curl -X POST http://localhost:8081/api/customer \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "username": "john",
    "password": "secret123"
  }'
```

Login:

```bash
curl -X POST http://localhost:8081/api/auth \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "secret123"
  }'
```

Get all customers:

```bash
curl http://localhost:8081/api/customer \
  -H "Authorization: Bearer <jwt-token>"
```

## Summary

Customer API provides a clean, production-oriented backend for customer registration, updates, lookup, and authentication using secure password handling and PostgreSQL persistence. It is small, easy to run locally, and documented through Swagger UI for rapid exploration and testing.
