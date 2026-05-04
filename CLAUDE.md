# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Event Management System (EMS) — a full-stack event ticketing platform. React + TypeScript frontend backed by three Java Spring Boot microservices (auth, event, order), all sharing one PostgreSQL instance via separate schemas.

## Commands

### Frontend (EMS_Frontend)

```bash
cd EMS_Frontend
npm run dev          # dev server on localhost:5173
npm run build        # production build (runs tsc -b first)
npm run lint         # ESLint check
npm run test         # Vitest single run (not watch mode)
npm run test -- src/path/to/file.test.tsx  # single test file
```

### Backend Services (each service directory)

Use `./mvnw` (Maven wrapper) instead of bare `mvn` — it's included in each service directory.

```bash
./mvnw spring-boot:run           # run service locally
./mvnw test                      # run all tests (uses H2 in-memory DB — no Postgres needed)
./mvnw test -Dtest=ClassName     # run a single test class
./mvnw package -DskipTests       # build JAR
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
| Auth Service | 8080 | Registration, login, JWT issuance, session validation |
| Event Management Service | 8081 | Event CRUD (`/api/organiser/*`), ticket tiers, public browsing |
| Order Service | 8082 | Order creation, order history, cancellation |
| Frontend (dev) | 5173 | React SPA |
| PostgreSQL | 5433 | Shared DB, separate schemas per service |

### Frontend Proxy (vite.config.ts)

All API calls go through Vite's dev proxy (same-origin from browser's perspective — required for cookies):
- `/api/auth/*` → `localhost:8080`
- `/api/orders/*` → `localhost:8082`
- `/api/organiser/*` → `localhost:8081`
- `/api/*` → `localhost:8081` (catch-all for public event endpoints)

In production, a reverse proxy must replicate this routing.

### Authentication Flow

1. `POST /api/auth/register` and `POST /api/auth/login` set an httpOnly `accessToken` cookie (24-hour lifetime, `SameSite=Lax`). No token is returned in the JSON body.
2. On app startup, the frontend calls `GET /api/auth/me` to restore session from the cookie. Failure silently clears local state and redirects to `/login`.
3. All three axios instances (`auth.ts`, `event.ts`, `order.ts`) use `withCredentials: true`. No Authorization headers are injected.
4. `POST /api/auth/logout` clears the cookie (sets `maxAge=0`).
5. The auth service's 401 response interceptor redirects to `/login` for any endpoint **except** `/api/auth/login` and `/api/auth/me` — excluding those two prevents an infinite reload loop on startup.
6. All protected event/order endpoints accept the token from either the `Authorization: Bearer` header **or** the `accessToken` cookie (cookie takes precedence when header is absent).
7. Two roles: `BUYER` and `ORGANISER`. Route guards in `App.tsx` (`ProtectedRoute`) enforce role-based access.
8. Login attempts are rate-limited: `RateLimitingService` allows max 10 attempts per IP per 15-minute sliding window (in-memory, resets on restart).

### Database Layout

Single PostgreSQL database (`eventplatform`) with three schemas:
- `auth` — users (includes `wallet_balance NUMERIC(10,2)` default `10000.00`)
- `events` — venues, events (DRAFT/PUBLISHED/CANCELLED), ticket_tiers
- `orders` — orders (PENDING/CONFIRMED/FAILED/CANCELLED), order_items

Cross-schema relationships are enforced at the application layer, not via foreign keys.

### Order Flow

1. Buyer calls `POST /api/orders` → Order Service validates tiers via `EventServiceClient` (synchronous HTTP to the event service), debits the buyer's wallet, and decrements ticket inventory using a conditional UPDATE (`remaining_qty >= requested`) to prevent oversell.
2. Order items snapshot event/tier data at purchase time for immutable history.
3. Buyer calls `POST /api/orders/{id}/cancel` → Order Service credits the wallet back and restores inventory (capped at `total_qty`). Cancellation is blocked within 24 hours of the event.

### Wallet

- `wallet_balance` lives in `auth.users` (migration `V4__Add_wallet_balance.sql`).
- The Order Service reads/writes it directly via `WalletRepository` (cross-schema query against `auth.users`).
- Both wallet debit and inventory decrement happen in a single `@Transactional` method — either both commit or both roll back.

### Frontend Structure

```
src/
├── App.tsx           # Route definitions + ProtectedRoute wrapper
├── contexts/         # AuthContext (session state via /me), CartContext (in-memory ticket cart)
├── services/         # Axios instances — auth.ts, event.ts, order.ts
├── views/            # Page-level components mapped to routes
├── components/       # Reusable UI (layouts, form elements, dialogs)
└── types/            # Shared TypeScript interfaces (auth.ts, event.ts, order.ts)
```

Key libraries: React Query (`@tanstack/react-query`) for server state, Axios for HTTP, Zod for runtime validation, React Router v7, Tailwind CSS v4.

`AuthContext.tsx` carries a `// @refresh reset` directive — it exports both a component and a hook, which breaks Vite Fast Refresh without this directive.

### Backend Layer Pattern (all three services)

```
Controller → Service → Repository (JPA) → PostgreSQL
```

Each service uses Flyway for schema migrations, `@ControllerAdvice` for centralized error handling, and explicit DTOs (no Lombok — all getters/setters written by hand). The event service has two controllers: `OrganiserEventController` (`/api/organiser/events`) for organiser operations and a public controller (`/api/events`) for browsing.

The Order Service has an additional `client/` package: `EventServiceClient` calls the Event Service synchronously over HTTP (Spring `RestClient`) to validate event data when creating orders. The Event Service URL must be reachable at order-creation time.

OpenAPI 3.0 specs:
- `auth-service/swagger/auth-openapi.yaml`
- `event_management_service/src/swagger/event-management-openapi.yaml`
- `order_service/swagger/order-service-openapi.yaml`

Backend package roots: `com.eventplatform.auth` (auth), `com.eventmanagement` (event), `com.ticketing.orderservice` (order).

## Environment Variables

Copy `.env.example` to `.env` at the project root before running `docker-compose`.

```
JWT_SECRET=          # Required. 256-bit secret shared across all services — no default
DATABASE_USERNAME=   # PostgreSQL username (default: postgres)
DATABASE_PASSWORD=   # PostgreSQL password (default: postgres)
JWT_ACCESS_EXPIRY=   # Access token lifetime ms (default: 86400000 = 24h)
CORS_ALLOWED_ORIGINS=http://localhost:5173
MOCK_PAYMENT_CHECKOUT=true   # Confirms orders immediately; set false for real payment integration
COOKIE_SECURE=false          # Set true when serving over HTTPS (enables Secure flag on cookie)
```

Each individual backend service also has a `.env.example` for running outside Docker.

## Testing

- **Frontend**: Vitest + jsdom. Test setup in `src/test/setup.ts`. Path alias `@/` resolves to `src/`.
- **Backend**: JUnit 5 + H2 in-memory database. No running PostgreSQL needed. `@WebMvcTest` slices use `@MockitoBean` for service/repository dependencies. `@DataJpaTest` tests use `@AutoConfigureTestDatabase(replace = NONE)` with H2 via the `test` profile.
- Backend tests: auth has `AuthServiceTest` + `AuthControllerTest`; event has `EventServiceTest` + `OrganiserEventControllerTest`; order has `OrderServiceTest` + `OrderRepositoryTest`.

## Git Workflow

After creating or modifying any files or folders in a turn, always:
1. Summarize every file that was created or changed.
2. Ask the user to review and confirm before any git operations.
3. Only if the user explicitly approves, proceed with: `git add` (specific files) → `git commit` → `git push`.

Never stage, commit, or push autonomously without explicit user approval in the same conversation turn.
