# Customer Service

Customer is the identity and customer-profile service for Mega Store. It provides public registration and login, protected customer management endpoints, BCrypt password hashing, and signed JWTs used by the Inventory and Order services.

## Features

- Register customers and manage customer profiles.
- Authenticate with a username and password; issue a signed JWT on successful login.
- Enforce unique usernames and email addresses.
- Hash passwords with BCrypt and never include password hashes in API responses.
- Include an `admin` flag in customer and authentication responses for role-aware clients such as the UI.
- Use optimistic locking to detect concurrent updates and return `409 Conflict`, allowing clients to reload and retry.
- Apply database schema migrations with Flyway.

## Technology and local dependencies

- Java 26, Spring Boot, Spring MVC, Spring Data JPA, and Maven
- PostgreSQL at `localhost:5434`, database `customer-service`
- Environment variables: `CUSTOMER_DB_USERNAME`, `CUSTOMER_DB_PASSWORD`, and `JWT_SECRET`

The database can be started with Docker Compose from the repository root; see the [project README](../README.md) for setup instructions. Flyway migrations run at service startup.

## Run

From the repository root:

```bash
./customer/mvnw -f customer/pom.xml spring-boot:run
```

The API listens on `http://localhost:8081`.

## API

Base URL: `http://localhost:8081`

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth` | Public | Authenticate and receive customer details and a JWT |
| `POST` | `/api/customer` | Public | Register a customer |
| `GET` | `/api/customer` | JWT | List customers |
| `GET` | `/api/customer/{id}` | JWT | Fetch a customer by UUID |
| `PUT` | `/api/customer` | JWT | Update a customer |
| `DELETE` | `/api/customer?id={id}` | JWT | Delete a customer |

Swagger UI: [http://localhost:8081/swagger-ui](http://localhost:8081/swagger-ui)
OpenAPI document: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

### Register

```http
POST /api/customer
Content-Type: application/json
```

```json
{
  "name": "Jordan Lee",
  "email": "jordan@example.com",
  "username": "jordan",
  "password": "secret123"
}
```

Name and username must be non-blank, email must be valid, and password length must be between 8 and 16 characters. A newly registered customer is not an administrator by default.

### Log in

```http
POST /api/auth
Content-Type: application/json
```

```json
{
  "username": "jordan",
  "password": "secret123"
}
```

The response contains a `customer` object (including its `admin` flag) and a `token`. Use the token to call protected endpoints:

```http
Authorization: Bearer <jwt-token>
```

### Update a customer

Send the customer UUID and updated profile fields. The password is optional; when supplied, it is validated and stored as a new BCrypt hash.

```json
{
  "id": "customer-uuid",
  "name": "Jordan Lee",
  "email": "jordan@example.com",
  "username": "jordan",
  "password": "newsecret123"
}
```

An update that would duplicate another customer's username or email is rejected. If another request updates the same customer concurrently, the API responds with `409 Conflict`.

## Security and configuration

- Login (`POST /api/auth`) and registration (`POST /api/customer`) are public. Other customer endpoints require a valid JWT.
- The JWT is signed using `JWT_SECRET` and expires after one hour by default.
- `JWT_SECRET` must match the value used by the Inventory and Order services.
- Configure database credentials with `CUSTOMER_DB_USERNAME` and `CUSTOMER_DB_PASSWORD`.
- Swagger and OpenAPI endpoints are enabled for local API exploration.

## Build and test

From the repository root:

```bash
./customer/mvnw -f customer/pom.xml test
./customer/mvnw -f customer/pom.xml clean package
```
