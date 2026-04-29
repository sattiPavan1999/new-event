# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Event Management System (EMS) — a full-stack event ticketing platform. React + TypeScript frontend backed by four Java Spring Boot microservices, all sharing one PostgreSQL instance via separate schemas.

## Commands

### Frontend (EMS_Frontend)

```bash
cd EMS_Frontend
npm run dev          # dev server on localhost:5173
npm run build        # production build
npm run lint         # ESLint check
npm run test         # Vitest (watch mode)
npm run test -- --run             # Vitest single run
npm run test -- --run src/path/to/file.test.tsx  # single test file
```

### Backend Services (each service directory)

```bash
mvn spring-boot:run           # run service locally
mvn test                      # run all tests
mvn test -Dtest=ClassName     # run a single test class
mvn package -DskipTests       # build JAR
```

### Full Stack

```bash
docker-compose up --build        # build and start all services
docker-compose up                # start all services
docker-compose down              # stop all services
docker-compose up -d postgres    # start only DB
docker-compose logs -f <service> # tail logs for a service
```

## Architecture

### Services and Ports

| Service | Port | Responsibility |
|---|---|---|
| Auth Service | 8080 | Registration, login, JWT issuance/refresh |
| Event Management Service | 8081 | Event CRUD (`/api/organiser/*`), ticket tiers, public browsing |
| Order Service | 8082 | Order creation, order history |
| Frontend (dev) | 5173 | React SPA |
| PostgreSQL | 5433 | Shared DB, separate schemas per service |

### Frontend Proxy (vite.config.ts)

All API calls go through Vite's dev proxy:
- `/api/auth/*` → `localhost:8080`
- `/api/orders/*` and `/api/payments/*` → `localhost:8082`
- `/api/organiser/*` → `localhost:8081` (organiser event management)
- `/api/*` → `localhost:8081` (catch-all for event service)

In production, a reverse proxy must replicate this routing.

### Authentication Flow

1. Auth service issues 24-hour JWT access tokens (no refresh tokens).
2. All protected backend endpoints validate JWT using a shared `JWT_SECRET`.
3. Frontend stores the access token in `AuthContext` (see `src/contexts/`) and injects the bearer header via service layer functions in `src/services/`.
4. Two roles: `BUYER` and `ORGANISER`. Route guards in `App.tsx` enforce role-based access.

### Database Layout

Single PostgreSQL database (`eventplatform`) with three schemas:
- `auth` — users (includes `wallet_balance`)
- `events` — venues, events (DRAFT/PUBLISHED/CANCELLED), ticket_tiers
- `orders` — orders (PENDING/CONFIRMED/FAILED/CANCELLED), order_items

Cross-schema relationships are enforced at the application layer, not via foreign keys.

### Order Flow

1. Buyer calls `POST /api/orders` → Order Service debits the buyer's wallet and decrements ticket inventory in the `events` schema.
2. Order items snapshot event/tier data at purchase time for immutable history.
3. Buyer calls `POST /api/orders/{id}/cancel` → Order Service credits the wallet back and restores inventory. Cancellation is blocked within 24 hours of the event.

### Wallet

- Every user has a `wallet_balance` column (`NUMERIC(10,2)`, default `10000.00`) in `auth.users` (added in migration `V4__Add_wallet_balance.sql`).
- The Order Service reads/writes the wallet directly via `WalletRepository` (cross-schema query against `auth.users`).
- Orders can have status `PENDING`, `CONFIRMED`, `FAILED`, or `CANCELLED` (constraint updated in migration `V5__Add_cancelled_order_status.sql`).

### Frontend Structure

```
src/
├── App.tsx           # Route definitions + ProtectedRoute wrapper
├── contexts/         # AuthContext (user state, token storage)
├── services/         # API fetch wrappers (one file per backend service)
├── views/            # Page-level components mapped to routes
├── components/       # Reusable UI (layouts, form elements, dialogs)
├── hooks/            # Custom React hooks
├── types/            # Shared TypeScript interfaces
└── constants/        # App-wide constants
```

### Backend Layer Pattern (all four services)

```
Controller → Service → Repository (JPA) → PostgreSQL
```

Each service uses Flyway for schema migrations, `@ControllerAdvice` for centralized error handling, and explicit DTOs (no Lombok — all getters/setters written by hand). The event service has two controllers: `OrganiserEventController` (`/api/organiser/events`) for organiser operations and a public controller (`/api/events`) for browsing.

## Environment Variables

Each backend service requires a `.env` file based on its `.env.example`. Key variables:

```
JWT_SECRET=          # 256-bit secret shared across all services
DB_URL=              # JDBC URL pointing to postgres:5433
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

## Testing

- **Frontend**: Vitest + jsdom. Test setup in `src/test/setup.ts`. Path alias `@/` resolves to `src/`.
- **Backend**: JUnit 5. Integration tests in each service's `src/test/` directory.
- **E2E**: Playwright config present in `EMS_Frontend/.playwright-mcp/`.
