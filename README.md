# Event Management System (EMS)

A full-stack event ticketing platform. Buyers can browse events and purchase tickets; organisers can create and manage events. Built with React + TypeScript on the frontend and three Java Spring Boot microservices on the backend, all backed by a single PostgreSQL database.

---

## Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start — Docker (Recommended)](#quick-start--docker-recommended)
- [Manual Setup — Run Services Locally](#manual-setup--run-services-locally)
- [Running Tests](#running-tests)
- [Environment Variables](#environment-variables)
- [API Overview](#api-overview)
- [User Roles](#user-roles)

---

## Architecture

| Service | Port | Responsibility |
|---|---|---|
| Auth Service | 8080 | Registration, login, session validation, JWT issuance |
| Event Management Service | 8081 | Event CRUD, ticket tiers, public browsing |
| Order Service | 8082 | Order creation, order history, cancellation |
| Frontend (dev) | 5173 | React SPA |
| PostgreSQL | 5433 | Shared DB — separate schemas per service |

**Single database, three schemas:**
- `auth` — users and wallet balances
- `events` — venues, events, ticket tiers
- `orders` — orders and order items

Cross-schema relationships are enforced at the application layer (no database-level foreign keys).

---

## Prerequisites

| Tool | Version |
|---|---|
| Docker & Docker Compose | any recent version |
| Java | 21 (for running services without Docker) |
| Maven | 3.9+ or use the included `./mvnw` wrapper |
| Node.js | 18+ (for running the frontend without Docker) |

---

## Quick Start — Docker (Recommended)

This is the fastest way to run the entire stack.

**1. Clone the repository**

```bash
git clone <repository-url>
cd new-event
```

**2. Create your `.env` file**

```bash
cp .env.example .env
```

Open `.env` and fill in the required value:

```
JWT_SECRET=your-256-bit-secret-here
```

All other values have sensible defaults (see [Environment Variables](#environment-variables)).

**3. Start all services**

```bash
docker-compose up --build
```

This starts PostgreSQL, all three backend services, and applies all database migrations automatically via Flyway.

**4. Start the frontend**

```bash
cd EMS_Frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

**Stop everything**

```bash
docker-compose down
```

---

## Manual Setup — Run Services Locally

Use this if you want to run individual services outside Docker (e.g. for faster iteration).

### 1. Start only the database

```bash
docker-compose up -d postgres
```

PostgreSQL will be available at `localhost:5433`.

### 2. Run a backend service

Each service directory (`auth-service`, `event_management_service`, `order_service`) contains its own `.env.example`. Copy it and fill in the values:

```bash
cp auth-service/.env.example auth-service/.env
# repeat for the other two services
```

Then run any service:

```bash
cd auth-service
./mvnw spring-boot:run
```

Repeat in separate terminals for `event_management_service` (binds to 8081) and `order_service` (binds to 8082).

### 3. Run the frontend

```bash
cd EMS_Frontend
npm install
npm run dev
```

The Vite dev server proxies API calls to the running backend services automatically.

---

## Running Tests

### Frontend

```bash
cd EMS_Frontend
npm run test                             # single run, all tests
npm run test -- src/path/to/file.test.tsx  # single test file
```

### Backend (each service)

```bash
cd auth-service          # or event_management_service / order_service
./mvnw test              # all tests
./mvnw test -Dtest=ClassName   # single test class
```

Backend tests use an H2 in-memory database — no running PostgreSQL needed.

---

## Environment Variables

Copy `.env.example` to `.env` at the project root before running `docker-compose`.

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | — | **Required.** 256-bit secret shared across all services |
| `DATABASE_USERNAME` | `postgres` | PostgreSQL username |
| `DATABASE_PASSWORD` | `postgres` | PostgreSQL password |
| `JWT_ACCESS_EXPIRY` | `86400000` | Access token lifetime in ms (24 hours) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed CORS origins |
| `MOCK_PAYMENT_CHECKOUT` | `true` | Confirms orders immediately without a payment provider |
| `COOKIE_SECURE` | `false` | Set `true` when serving over HTTPS (adds `Secure` flag to auth cookie) |

> Each service also has its own `.env.example` for running outside Docker. Variable names match the table above.

---

## API Overview

All API calls from the frontend go through Vite's dev proxy (configured in `EMS_Frontend/vite.config.ts`):

| Path prefix | Routes to |
|---|---|
| `/api/auth/*` | Auth Service — port 8080 |
| `/api/orders/*` | Order Service — port 8082 |
| `/api/organiser/*` | Event Service — port 8081 (organiser management) |
| `/api/*` | Event Service — port 8081 (public browsing) |

### Key endpoints

**Auth**

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register as BUYER or ORGANISER; sets httpOnly `accessToken` cookie |
| POST | `/api/auth/login` | Login; sets httpOnly `accessToken` cookie |
| GET | `/api/auth/me` | Return current user from cookie (used for session restore on page load) |
| POST | `/api/auth/logout` | Clear the auth cookie |

**Events (public)**

| Method | Path | Description |
|---|---|---|
| GET | `/api/events` | Browse published events |
| GET | `/api/events/{id}` | View event detail |

**Events (organiser — requires ORGANISER role)**

| Method | Path | Description |
|---|---|---|
| POST | `/api/organiser/events` | Create event |
| PUT | `/api/organiser/events/{id}` | Update event |
| PATCH | `/api/organiser/events/{id}/publish` | Publish event |
| PATCH | `/api/organiser/events/{id}/cancel` | Cancel event |
| POST | `/api/organiser/events/{id}/tiers` | Add ticket tier |
| DELETE | `/api/organiser/events/{id}/tiers/{tierId}` | Delete tier |
| GET | `/api/organiser/events/{id}/summary` | Sales summary |

**Orders (requires BUYER role)**

| Method | Path | Description |
|---|---|---|
| POST | `/api/orders` | Create order (debits wallet) |
| GET | `/api/orders/my` | Order history |
| GET | `/api/orders/{id}` | Single order detail |
| POST | `/api/orders/{id}/cancel` | Cancel order (refunds wallet, blocked within 24h of event) |

**Health checks** (all services)

```
GET /actuator/health
```

---

## User Roles

| Role | Can do |
|---|---|
| `BUYER` | Browse events, purchase tickets, view/cancel own orders |
| `ORGANISER` | Create and manage events, configure ticket tiers, view sales reports |

Every registered user gets a wallet with a default balance of **10,000.00**. Orders debit the wallet directly — no external payment integration required.

---

## Project Structure

```
new-event/
├── auth-service/               # Spring Boot — authentication & JWT
├── event_management_service/   # Spring Boot — event & tier management
├── order_service/              # Spring Boot — orders & wallet
├── EMS_Frontend/               # React + TypeScript + Vite
├── docker-compose.yml
└── .env.example
```

Each backend service follows the same layered pattern:

```
Controller → Service → Repository (JPA) → PostgreSQL
```

Detailed API specifications (OpenAPI 3.0) are in each service's `swagger/` directory.
