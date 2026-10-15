# Store UI

The `ui` directory contains the browser-based storefront and administration interface for Mega Store. It is a single-page application built with Angular 22, TypeScript, and RxJS. The UI communicates with the Customer, Inventory, and Order microservices over HTTP; it does not provide a backend of its own.

## Features

- Customer sign-up and login.
- Product browsing, shopping cart, checkout, and order history, including order cancellation where allowed.
- Admin pages for managing products, users, and orders.
- JWT-based authentication. After login, the UI stores the session in browser `sessionStorage` and attaches the token to API requests automatically.

The main routes are `/` (login), `/signup`, `/home`, `/cart`, `/myOrders`, and `/admin` with product, user, and order management pages.

## Requirements

- Node.js compatible with Angular CLI 22 (Node.js 22 or newer is recommended).
- npm (the project declares npm 12.2.0).
- The Mega Store backend services for API-backed features:
  - Inventory on `http://localhost:8080`
  - Customer on `http://localhost:8081`
  - Order on `http://localhost:8082`
- The backend's required infrastructure, such as PostgreSQL and Kafka, as described in the repository's [main README](../README.md).

The Angular development server proxies API requests, so no UI environment file or direct browser-to-service CORS configuration is needed:

| UI request path | Backend |
| --- | --- |
| `/api/product`, `/api/stock` | Inventory (`localhost:8080`) |
| `/api/cart` | Order (`localhost:8082`) |
| Other `/api` requests, including authentication and customers | Customer (`localhost:8081`) |

## Run locally

From the repository root:

```bash
cd ui
npm ci
npm start
```

Open [http://localhost:4200](http://localhost:4200). Keep the backend services running on the ports listed above to use login, product, cart, order, and administration features.

## Available commands

Run these from `ui/`:

| Command | Description |
| --- | --- |
| `npm start` | Start the Angular development server with the API proxy. |
| `npm run build` | Build the UI for production. |
| `npm run watch` | Build in development mode and rebuild when files change. |
| `npm test` | Run the UI tests. |

The unit tests cover cart state and persistence, session storage, authorization headers, and the quantity stepper component. They run with Vitest in a jsdom environment.

## Project structure

- `src/app/home`, `cart`, `my-orders`, `login`, and `signup` contain the customer-facing pages.
- `src/app/admin` contains the administration pages.
- `src/app/app.routes.ts` defines the client-side routes.
- `src/app/cart.service.ts` manages the cart in browser session storage.
- `src/app/session.ts` and `src/app/auth.interceptor.ts` manage the login session and authorization header.
- `proxy.conf.json` routes local API requests to the backend services.
