# Project Files Reference

Event Management System (EMS) — full-stack event ticketing platform.
React + TypeScript frontend backed by four Java Spring Boot microservices sharing one PostgreSQL instance via separate schemas.

---

## Root

| File | Purpose |
|---|---|
| `docker-compose.yml` | Orchestrates all 5 containers (postgres, auth, event, order, payment) with health checks, port mappings, and shared network |
| `CLAUDE.md` | Project instructions for Claude Code: commands, architecture, DB layout, env vars, testing |
| `PROJECT_FILES.md` | This file — maps every file to its role in the project |
| `.claude/settings.local.json` | Claude Code local permission and hook settings |

---

## Frontend — `EMS_Frontend/`

React 19 + TypeScript 6 + Vite SPA. Dev proxy in `vite.config.ts` routes all `/api/*` calls to the appropriate backend service.

### Config

| File | Purpose |
|---|---|
| `package.json` | Dependencies: React 19, TanStack Query, Axios, React Router v7, Zod, Tailwind CSS 4, Vite, Vitest |
| `package-lock.json` | Locked dependency tree |
| `vite.config.ts` | Dev proxy (`/api/auth` → 8080, `/api/orders` → 8082, `/api/*` → 8081); Tailwind PostCSS; `@/` path alias |
| `tsconfig.json` | Root TypeScript project references |
| `tsconfig.app.json` | TypeScript config for `src/` (strict, bundler module resolution) |
| `tsconfig.node.json` | TypeScript config for Vite config file |
| `eslint.config.js` | ESLint with oxc parser for TypeScript/React |
| `vitest.config.ts` | Vitest setup: jsdom environment, `@/` alias, setup file |
| `index.html` | HTML entry point — mounts `<div id="root">` |
| `.gitignore` | Ignores `node_modules/`, `dist/`, `.env`, coverage, OS files |

### Source — `src/`

#### Entry Points

| File | Purpose |
|---|---|
| `main.tsx` | Renders `<App />` into `#root` with React strict mode |
| `App.tsx` | Route definitions, `ProtectedRoute` wrapper, role-based redirects (BUYER / ORGANISER) |
| `index.css` | Global Tailwind directives and base styles |
| `App.css` | Sets `#root` to full-width and full-viewport-height |

#### Contexts — `src/contexts/`

| File | Purpose |
|---|---|
| `AuthContext.tsx` | `useAuth()` hook + `AuthProvider`; manages user state, access/refresh tokens (localStorage), wallet balance; exposes login, logout, updateWalletBalance |

#### Services — `src/services/`

API fetch wrappers with JWT injection via Axios interceptors.

| File | Purpose |
|---|---|
| `auth.ts` | Axios instance for `localhost:8080`; login, register, logout, refresh token |
| `event.ts` | Axios instance for `localhost:8081`; browse events, event detail, organiser CRUD |
| `order.ts` | Axios instance for `localhost:8082`; create order, order history, order detail, cancel order |

#### Types — `src/types/`

| File | Purpose |
|---|---|
| `auth.ts` | `User`, `AuthResponse`, `LoginRequest`, `RegisterRequest`, `LogoutRequest` |
| `event.ts` | `Event`, `EventCategory`, `EventStatus`, `TicketTier`, `Venue` |
| `order.ts` | `Order`, `OrderItem`, `CreateOrderRequest`, `OrderStatus` |

#### Components — `src/components/`

Reusable UI building blocks. Each component lives in its own folder with an `index.ts` barrel export.

| Folder | Component File | Test File | Purpose |
|---|---|---|---|
| `alert/` | `alert.tsx` | `alert.test.tsx` | Dismissible notification / error banner |
| `button/` | `button.tsx` | `button.test.tsx` | Styled button with variant and loading state |
| `input/` | `input.tsx` | `input.test.tsx` | Form input field with label and error display |
| `buyer-layout/` | `buyer-layout.tsx` | `buyer-layout.test.tsx` | Page wrapper for buyer routes (nav, wallet display) |
| `organiser-layout/` | `organiser-layout.tsx` | `organiser-layout.test.tsx` | Page wrapper for organiser routes (nav, sidebar) |
| `logout-dialog/` | `logout-dialog.tsx` | `logout-dialog.test.tsx` | Confirmation modal for logout action |

#### Views — `src/views/`

Page-level components mapped 1:1 to routes in `App.tsx`. Each folder has an `index.ts` barrel export.

| Folder | View File | Test File(s) | Purpose |
|---|---|---|---|
| `login/` | `login.view.tsx` | `login.test.tsx` | Login form for both roles; redirects to role dashboard |
| `buyer-registration/` | `buyer-registration.view.tsx` | `buyer-registration.test.tsx` | BUYER signup with Zod validation |
| `organiser-registration/` | `organiser-registration.view.tsx` | — | ORGANISER signup form |
| `event-listing/` | `event-listing.view.tsx` | — | Public event browser with category, city, and text filters |
| `event-detail/` | `event-detail.view.tsx` | `event-detail.view.test.tsx` | Public event detail page; ticket tier selection and purchase |
| `order-confirmation/` | `order-confirmation.view.tsx` | `order-confirmation.test.tsx` | Success page shown after order is placed |
| `my-bookings/` | `my-bookings.view.tsx` | `my-bookings.test.tsx`, `my-bookings.view.test.tsx` | Buyer's paginated order history with cancel button |
| `organiser-dashboard/` | `organiser-dashboard.view.tsx` | `organiser-dashboard.test.tsx` | Organiser home: revenue, ticket sales, event count |
| `organiser-events/` | `organiser-events.view.tsx` | — | Organiser's event list filtered by status |
| `organiser-event-detail/` | `organiser-event-detail.view.tsx` | — | Organiser view of one event: tier management, publish action |
| `organiser-event-create/` | `organiser-event-create.view.tsx` | — | Event creation form: title, category, venue, date, tiers |

#### Test Setup — `src/test/`

| File | Purpose |
|---|---|
| `setup.ts` | Vitest global setup: jsdom mocks, `@testing-library/jest-dom` matchers |

#### Assets — `src/assets/`

| File | Purpose |
|---|---|
| `hero.png` | Hero image used on the event listing landing page |

#### Public — `public/`

| File | Purpose |
|---|---|
| `favicon.svg` | Browser tab icon |
| `icons.svg` | Icon sprite sheet referenced throughout the UI |

---

## Auth Service — `auth-service/`

Spring Boot 3.5.14 — port **8080**. Handles registration, login, JWT issuance, refresh tokens, and logout.

### Config & Build

| File | Purpose |
|---|---|
| `pom.xml` | Maven build: Spring Boot 3.5.14, Spring Data JPA, Spring Validation, jjwt 0.12.5, BCrypt, PostgreSQL driver, Flyway |
| `Dockerfile` | Multi-stage build; copies JAR and exposes port 8080 |
| `mvnw` / `mvnw.cmd` | Maven wrapper (Linux/Windows) |
| `src/main/resources/application.yml` | Server port 8080, JDBC URL, Flyway schema `auth`, JWT secret + expiry (900s access / 7d refresh) |
| `src/test/resources/application-test.yml` | Test profile overrides (H2 or mock DB) |
| `swagger/auth-openapi.yaml` | OpenAPI 3.0 spec for auth endpoints |
| `README.md` | Service-level documentation |

### Database Migrations — `src/main/resources/db/migration/auth/`

| File | SQL |
|---|---|
| `V1__create_auth_schema.sql` | Creates `auth` schema |
| `V2__create_users_table.sql` | `auth.users`: UUID PK, email (UNIQUE), password_hash, full_name, role (BUYER/ORGANISER), is_active, created_at; indexes |
| `V3__create_refresh_tokens_table.sql` | `auth.refresh_tokens`: UUID PK, user_id FK, token, expiry, created_at |
| `V4__Add_wallet_balance.sql` | Adds `wallet_balance NUMERIC(10,2) DEFAULT 10000.00` to `auth.users` |

### Java Source — `src/main/java/com/eventplatform/auth/`

| File | Purpose |
|---|---|
| `AuthApplication.java` | `@SpringBootApplication` entry point |
| **controller/** `AuthController.java` | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` |
| **service/** `AuthService.java` | Register (BCrypt hash), login (validate + issue tokens), refresh token, logout (delete refresh token) |
| **service/** `AuditService.java` | Logs auth events for audit trail |
| **repository/** `UserRepository.java` | JPA: `findByEmail`, CRUD on `auth.users` |
| **repository/** `RefreshTokenRepository.java` | JPA: find by token, delete by user_id |
| **entity/** `User.java` | JPA entity → `auth.users` |
| **entity/** `RefreshToken.java` | JPA entity → `auth.refresh_tokens` |
| **enums/** `UserRole.java` | `BUYER`, `ORGANISER` |
| **dto/** `LoginRequest.java` | email + password |
| **dto/** `RegisterRequest.java` | email, password, full_name, role |
| **dto/** `RefreshRequest.java` | refresh_token |
| **dto/** `LogoutRequest.java` | user_id |
| **dto/** `AuthResponse.java` | accessToken + refreshToken + UserDto |
| **dto/** `LogoutResponse.java` | success message |
| **dto/** `UserDto.java` | id, email, full_name, role, wallet_balance |
| **dto/** `ErrorResponse.java` | status, message, timestamp |
| **exception/** `GlobalExceptionHandler.java` | `@ControllerAdvice` — maps all exceptions to HTTP responses |
| **exception/** `DuplicateEmailException.java` | Email already registered |
| **exception/** `InvalidCredentialsException.java` | Wrong email or password |
| **exception/** `InvalidTokenException.java` | Malformed or expired JWT |
| **exception/** `InvalidRoleException.java` | Unknown role in token or request |
| **config/** `JwtConfig.java` | Spring beans: JWT secret and expiry values |
| **config/** `CorsConfig.java` | Allows requests from `CORS_ALLOWED_ORIGINS` |
| **config/** `MdcFilter.java` | Adds trace ID to MDC for structured logging |
| **util/** `JwtUtil.java` | JWT creation, validation, and claim extraction (userId, role) using jjwt |

### Tests — `src/test/java/`

| File | Purpose |
|---|---|
| `AuthControllerTest.java` | Controller endpoint unit tests |
| `AuthServiceTest.java` | Service logic unit tests |
| `AuthServiceIntegrationTest.java` | Integration tests with mock/H2 DB |
| `JwtUtilTest.java` | JWT generation and validation tests |
| `UserRepositoryTest.java` | Repository query tests |
| `RefreshTokenRepositoryTest.java` | Refresh token repository tests |

---

## Event Management Service — `event_management_service/`

Spring Boot 3.5.14 — port **8081**. Event CRUD, ticket tier management, venue lookup, and public event browsing.

### Config & Build

| File | Purpose |
|---|---|
| `pom.xml` | Maven build: Spring Boot 3.5.14, Spring Data JPA, Validation, PostgreSQL, Flyway, H2 (test) |
| `Dockerfile` | Builds and exposes port 8080 (mapped to 8081 by Compose) |
| `mvnw` / `mvnw.cmd` | Maven wrapper |
| `src/main/resources/application.yml` | Port 8081, JDBC URL, Flyway schema `events`, HikariCP pool 10, graceful shutdown |
| `src/main/resources/application-test.yml` | H2 in-memory DB for tests |
| `src/swagger/event-management-openapi.yaml` | OpenAPI 3.0 spec |
| `README.md` | Service-level documentation |

### Database Migrations — `src/main/resources/db/migration/event/`

| File | SQL |
|---|---|
| `V1__create_schema.sql` | Creates `events` schema; `venues`, `events`, `ticket_tiers` tables with indexes on status, date, category, organiser_id |
| `V2__insert_sample_venues.sql` | Seed data: sample venues for local development and demos |

### Java Source — `src/main/java/com/eventmanagement/`

| File | Purpose |
|---|---|
| `EventManagementServiceApplication.java` | `@SpringBootApplication` entry point |
| **controller/** `OrganiserEventController.java` | `GET/POST /api/organiser/events`, `GET/POST/PUT/PATCH/DELETE /api/organiser/events/{id}/...` — all operations require `X-User-Id` header |
| **controller/** `PublicEventController.java` | `GET /api/events` (paginated browse with filters), `GET /api/events/{id}` — no auth required |
| **controller/** `VenueController.java` | `GET /api/venues` — list all venues for event creation form |
| **service/** `EventService.java` | Create event, add/update/delete tier, publish event, browse with filters, get detail, sales summary |
| **service/** `AuditService.java` | Audit trail for organiser actions |
| **repository/** `EventRepository.java` | JPA: find by organiser_id, paginated browse by status/category/city/search |
| **repository/** `TicketTierRepository.java` | JPA: find tiers by event_id, decrement/restore quantities |
| **repository/** `VenueRepository.java` | JPA: find by id, list all, find by city |
| **entity/** `Event.java` | JPA entity → `events.events`; `@ManyToOne` Venue, `@OneToMany` TicketTiers |
| **entity/** `TicketTier.java` | JPA entity → `events.ticket_tiers`; `@ManyToOne` Event |
| **entity/** `Venue.java` | JPA entity → `events.venues` |
| **enums/** `EventCategory.java` | `CONCERT`, `SPORTS`, `CONFERENCE`, `OTHER` |
| **enums/** `EventStatus.java` | `DRAFT`, `PUBLISHED`, `CANCELLED` |
| **enums/** `TierStatus.java` | `ACTIVE`, `CLOSED`, `SOLD_OUT` |
| **dto/** `CreateEventRequest.java` | title, description, category, event_date, venue_id, banner_image_url |
| **dto/** `CreateTierRequest.java` | name, description, price, total_qty, max_per_order |
| **dto/** `EventResponse.java` | Organiser event list row: id, title, status, created_at |
| **dto/** `EventDetailResponse.java` | Full event with venue and tiers list |
| **dto/** `EventSummaryResponse.java` | Public card: title, date, venue, cheapest tier price, remaining count |
| **dto/** `TierResponse.java` | Tier detail: id, name, price, total_qty, remaining_qty, status |
| **dto/** `VenueDto.java` | id, name, address, city, capacity |
| **dto/** `SalesSummaryResponse.java` | total_orders, revenue, tickets_sold (organiser dashboard) |
| **dto/** `PageResponse.java` | Generic paginated wrapper (content, page, total) |
| **dto/** `ErrorResponse.java` | status, message, timestamp |
| **exception/** `GlobalExceptionHandler.java` | `@ControllerAdvice` for all exceptions |
| **exception/** `ResourceNotFoundException.java` | Event, tier, or venue not found |
| **exception/** `UnauthorizedException.java` | Missing or invalid auth header |
| **exception/** `ForbiddenException.java` | Organiser not owner of requested event |
| **exception/** `BusinessRuleViolationException.java` | Cannot publish without tiers; cannot edit published event |
| **config/** `CorsConfig.java` | Allows requests from `CORS_ALLOWED_ORIGINS` |

### Tests — `src/test/java/`

| File | Purpose |
|---|---|
| `OrganiserEventControllerTest.java` | Organiser endpoint tests |
| `PublicEventControllerTest.java` | Public browse and detail endpoint tests |
| `EventServiceTest.java` | Service logic tests |
| `EventRepositoryTest.java` | Repository query tests |

---

## Order Service — `order_service/`

Spring Boot 3.5.14 — port **8082**. Order creation (wallet debit + inventory lock), order history, and cancellation (wallet refund + inventory restore).

### Config & Build

| File | Purpose |
|---|---|
| `pom.xml` | Maven build: Spring Boot 3.5.14, JPA, Validation, JWT, PostgreSQL, Flyway, H2 (test) |
| `.env.example` | Template for required env vars: `JWT_SECRET`, `DB_URL`, `CORS_ALLOWED_ORIGINS`, event service URL, mock payment flag |
| `Dockerfile` | Builds and exposes port 8080 (mapped to 8082 by Compose) |
| `mvnw` / `mvnw.cmd` | Maven wrapper |
| `src/main/resources/application.properties` | Port 8082, JDBC URL, Flyway schema `orders`, JWT secret, event service URL, mock payment mode |
| `src/test/resources/application-test.properties` | Test profile overrides |
| `swagger/order-service-openapi.yaml` | OpenAPI 3.0 spec |
| `README.md` | Service-level documentation |

### Database Migrations — `src/main/resources/db/migration/order/`

| File | SQL |
|---|---|
| `V1__Create_orders_schema.sql` | Creates `orders` schema; `orders` (id, buyer_id, status CHECK, total_amount, payment_link_id, timestamps) and `order_items` (order_id FK, tier_id, tier_name, event_title, event_date, quantity, unit_price) tables |
| `V2__Rename_stripe_to_razorpay.sql` | Renames payment column from Stripe naming to Razorpay naming (legacy provider switch) |
| `V3__Add_venue_name_to_order_items.sql` | Adds `venue_name` to `order_items` for immutable order record |
| `V4__Rename_razorpay_payment_link_id.sql` | Renames Razorpay column to generic `payment_link_id` |
| `V5__Add_cancelled_order_status.sql` | Adds `CANCELLED` to the `status` CHECK constraint on `orders` |

### Java Source — `src/main/java/com/ticketing/orderservice/`

| File | Purpose |
|---|---|
| `OrderServiceApplication.java` | `@SpringBootApplication` entry point |
| **controller/** `OrderController.java` | `POST /api/orders`, `GET /api/orders/my`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel` — all require BUYER JWT |
| **controller/** `PaymentWebhookController.java` | `POST /api/webhooks/payment` — receives payment status updates from payment provider |
| **service/** `OrderService.java` | Create order (validate inventory → debit wallet → persist order + items); cancel order (check 24h window → refund wallet → restore inventory); fetch history and detail |
| **service/** `PaymentWebhookService.java` | Processes incoming webhook to update order from PENDING → CONFIRMED or FAILED |
| **service/** `AuditService.java` | Audit trail for order events |
| **repository/** `OrderRepository.java` | JPA: find by buyer_id (paginated), find by id, save |
| **repository/** `OrderItemRepository.java` | JPA: find by order_id, save items |
| **repository/** `TicketTierRepository.java` | JPA: reads `events.ticket_tiers` (cross-schema) for inventory validation |
| **repository/** `WalletRepository.java` | Cross-schema JPA: reads and writes `auth.users.wallet_balance` |
| **entity/** `Order.java` | JPA entity → `orders.orders`; `@OneToMany` OrderItems |
| **entity/** `OrderItem.java` | JPA entity → `orders.order_items`; snapshots tier/event data at purchase time |
| **entity/** `OrderStatus.java` | Enum: `PENDING`, `CONFIRMED`, `FAILED`, `CANCELLED` |
| **client/** `EventServiceClient.java` | HTTP client (RestTemplate) calling event service (port 8081) to validate tier existence and pricing |
| **dto/** `CreateOrderRequest.java` | List of `OrderItemRequest` (tier_id, quantity) |
| **dto/** `OrderItemRequest.java` | tier_id, quantity |
| **dto/** `CreateOrderResponse.java` | order id, status, payment_link_id (null in mock mode) |
| **dto/** `OrderDetailResponse.java` | Full order: id, buyer_id, status, total_amount, items, created_at |
| **dto/** `OrderItemDetail.java` | Per-item: tier_name, event_title, event_date, venue_name, quantity, unit_price, subtotal |
| **dto/** `OrderHistoryResponse.java` | Paginated list of `OrderSummary` |
| **dto/** `OrderSummary.java` | id, status, total_amount, created_at, first event name |
| **dto/** `OrderItemSummary.java` | tier_name, quantity, unit_price |
| **dto/** `CancelOrderResponse.java` | Success message + refunded amount |
| **dto/** `EventServiceResponse.java` | Tier data returned from event service |
| **dto/** `WebhookResponse.java` | Incoming payment webhook payload |
| **dto/** `ErrorResponse.java` | status, message, timestamp |
| **exception/** `GlobalExceptionHandler.java` | `@ControllerAdvice` for all exceptions |
| **exception/** `OrderNotFoundException.java` | Order not found by id |
| **exception/** `OrderAccessDeniedException.java` | Buyer accessing another buyer's order |
| **exception/** `TierNotFoundException.java` | Tier not found in event service |
| **exception/** `EventNotFoundException.java` | Event not found |
| **exception/** `InsufficientInventoryException.java` | Tier sold out or requested qty > remaining |
| **exception/** `QuantityExceedsMaxPerOrderException.java` | qty > tier's max_per_order limit |
| **exception/** `InsufficientWalletBalanceException.java` | Buyer wallet balance too low |
| **exception/** `InvalidEventStatusException.java` | Event is DRAFT or CANCELLED |
| **exception/** `InvalidTierStatusException.java` | Tier is CLOSED or SOLD_OUT |
| **exception/** `OrderCancellationNotAllowedException.java` | Event is within 24 hours |
| **exception/** `PaymentServiceException.java` | Payment provider error |
| **exception/** `UnauthorizedException.java` | Missing or invalid JWT |
| **config/** `EventServiceConfig.java` | `RestTemplate` bean for outbound calls to event service |
| **config/** `CorsConfig.java` | Allows requests from `CORS_ALLOWED_ORIGINS` |
| **config/** `MdcFilter.java` | Adds trace ID to MDC for structured logging |
| **util/** `JwtUtil.java` | JWT validation, extract buyer_id, assert BUYER role |

### Tests — `src/test/java/`

| File | Purpose |
|---|---|
| `OrderControllerTest.java` | Controller endpoint tests |
| `OrderServiceTest.java` | Service logic tests (create, cancel, history) |
| `OrderRepositoryTest.java` | Repository query tests |

---

## Payment Service — `payment_service/`

Spring Boot 3.5.14 — port **8083**. Thin webhook-handling service; no DB. Payment creation is mocked in order service via `MOCK_PAYMENT=true`.

### Config & Build

| File | Purpose |
|---|---|
| `pom.xml` | Maven build: Spring Boot 3.5.14, Actuator, Jackson; no DB dependencies |
| `.env.example` | Template: `WEBHOOK_SECRET`, `SERVER_PORT` |
| `Dockerfile` | Builds and exposes port 8080 (mapped to 8083 by Compose) |
| `mvnw` / `mvnw.cmd` | Maven wrapper |
| `src/main/resources/application.properties` | Port 8083, webhook secret, CORS origins, logging |
| `swagger/payment-openapi.yaml` | OpenAPI 3.0 spec |

### Java Source — `src/main/java/com/ticketing/payment/`

| File | Purpose |
|---|---|
| `PaymentServiceApplication.java` | `@SpringBootApplication` entry point |
| **config/** `WebConfig.java` | CORS and Jackson configuration |
| **config/** `MDCFilter.java` | Trace ID logging filter |

### Tests — `src/test/java/`

| File | Purpose |
|---|---|
| `MDCFilterTest.java` | Tests MDC filter request/response trace ID injection |

---

## Database Schema Summary

Single PostgreSQL instance (`eventplatform`) — three schemas:

### `auth` schema
| Table | Columns |
|---|---|
| `users` | UUID id, email (UNIQUE), password_hash, full_name, role, is_active, wallet_balance (default 10000.00), created_at |
| `refresh_tokens` | UUID id, user_id FK → users, token, expiry, created_at |

### `events` schema
| Table | Columns |
|---|---|
| `venues` | UUID id, name, address, city, country, capacity |
| `events` | UUID id, organiser_id, venue_id FK → venues, title, description, category, event_date, banner_image_url, status, created_at, updated_at |
| `ticket_tiers` | UUID id, event_id FK → events, name, description, price, total_qty, remaining_qty, max_per_order, sale_start, sale_end, status, created_at |

### `orders` schema
| Table | Columns |
|---|---|
| `orders` | UUID id, buyer_id, status (PENDING/CONFIRMED/FAILED/CANCELLED), total_amount, payment_link_id, created_at, updated_at |
| `order_items` | UUID id, order_id FK → orders, tier_id, tier_name, event_title, event_date, quantity, unit_price, venue_name, created_at |

Cross-schema reads: Order Service reads `events.ticket_tiers` for inventory and `auth.users` for wallet — enforced at application layer, not by FK constraints.
