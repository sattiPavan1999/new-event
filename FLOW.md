# EMS — Complete Application Flow

End-to-end flow from opening the UI to logout, covering token generation, storage, injection, validation, and every major user action.

---

## 1. System Overview

```
Browser (React SPA :5173)
        │
        │  /api/auth/*  ──────────────────────► Auth Service :8080
        │                                              │
        │  /api/organiser/* ──────────────────► Event Service :8081
        │  /api/events/*                               │
        │                                        PostgreSQL :5433
        │  /api/orders/* ────────────────────► Order Service :8082
        │  /api/payments/*                             │
        │                                        (cross-schema reads)
        │
   Vite dev proxy routes all /api/* calls to the correct backend
```

All four services share the **same `JWT_SECRET`**. No service calls Auth Service to validate a token — each one verifies the signature independently.

---

## 2. App Startup (Page Load)

```
Browser loads index.html
        │
        ▼
React mounts: QueryClientProvider → AuthProvider → BrowserRouter → AppRoutes
        │
        ▼
AuthProvider constructor (useState initializers)
        ├── reads localStorage("user")      → sets user state
        └── reads localStorage("accessToken") → sets accessToken state
        │
        ▼
AuthProvider useEffect (runs once on mount)
        ├── setAuthToken(localStorage("accessToken"))    → auth.ts module var
        ├── setEventApiToken(localStorage("accessToken")) → event.ts module var
        └── setOrderApiToken(localStorage("accessToken")) → order.ts module var
        │
        ▼
AppRoutes checks: isAuthenticated = (user != null && accessToken != null)
        │
        ├── NOT authenticated → redirect to /login
        │
        ├── authenticated + BUYER → redirect to /events
        │
        └── authenticated + ORGANISER → redirect to /organiser/dashboard
```

---

## 3. Registration Flow

```
User fills Registration form (/register/buyer  or  /register/organiser)
        │
        ▼
Frontend validation (inline, before API call)
        ├── Email: required + valid format
        ├── Full Name: required
        └── Password: required
        │
        ▼  POST /api/auth/register  { email, fullName, password, role }
        │
        ▼  Vite proxy forwards to Auth Service :8080
        │
        ▼  AuthController.register()
        │
        ▼  AuthService.register()
        ├── userRepository.existsByEmail() → if duplicate → 409 DuplicateEmailException
        ├── UserRole.valueOf(role)         → if invalid   → 400 InvalidRoleException
        ├── BCrypt.encode(password, strength=12) → passwordHash
        ├── new User { id=UUID, email, passwordHash, fullName, role, isActive=true,
        │             walletBalance=10000.00, createdAt=now }
        ├── userRepository.save(user)
        ├── jwtUtil.generateAccessToken(userId, email, role)  ← TOKEN CREATED HERE
        └── auditService.logRegistration(email, role, success)
        │
        ▼
JWT generated (see Section 5 for structure)
        │
        ▼  Response: { accessToken, user: { id, email, fullName, role, walletBalance } }
        │
        ▼
Frontend: login(authData) called on AuthContext
        ├── setUser(authData.user)           → React state
        ├── setAccessToken(authData.accessToken) → React state
        ├── setAuthToken(token)              → auth.ts module var
        ├── setEventApiToken(token)          → event.ts module var
        ├── setOrderApiToken(token)          → order.ts module var
        ├── localStorage.setItem("user", JSON.stringify(user))
        └── localStorage.setItem("accessToken", token)
        │
        ▼
navigate to /events (BUYER) or /organiser/dashboard (ORGANISER)
```

---

## 4. Login Flow

```
User fills Login form (/login)
        │
        ▼
Frontend validation
        ├── Email: required + valid format
        └── Password: required
        │
        ▼  POST /api/auth/login  { email, password }
        │
        ▼  AuthService.login()
        ├── userRepository.findByEmail(email)
        │
        ├── [User NOT FOUND]
        │       ├── BCrypt.matches(password, dummyHash)  ← timing attack prevention
        │       │   (same BCrypt cost so response time is identical whether user exists or not)
        │       └── throw InvalidCredentialsException("Invalid email or password")
        │
        └── [User FOUND]
                ├── BCrypt.matches(password, user.passwordHash)
                │       └── [WRONG PASSWORD] → throw InvalidCredentialsException
                ├── jwtUtil.generateAccessToken(userId, email, role)  ← TOKEN CREATED
                └── auditService.logLogin(email, success=true)
        │
        ▼  Response: { accessToken, user: { id, email, fullName, role, walletBalance } }
        │
        ▼
Same as Registration: token + user stored in React state and localStorage
        │
        ▼
navigate based on role: BUYER → /events,  ORGANISER → /organiser/dashboard
```

---

## 5. JWT — Structure and Generation

### How the token is built (Auth Service — `JwtUtil.generateAccessToken`)

```
Header (base64)          Payload (base64)               Signature
─────────────────────    ──────────────────────────    ──────────────────────────
{                        {                             HMAC-SHA256(
  "alg": "HS256"           "sub": "<userId UUID>",       header + "." + payload,
}                          "email": "user@x.com",        JWT_SECRET
                           "role": "BUYER",            )
                           "type": "access",
                           "iat": <issued-at unix>,
                           "exp": <expiry unix>        ← 86400000ms = 24 hours
                         }
```

### Token lifetime

```
Token issued at:   T
Token expires at:  T + 24 hours
No refresh tokens — user must log in again after 24 hours
```

### Why all services can validate without calling Auth Service

```
All services start with:
    SecretKey = HMAC key derived from JWT_SECRET (same value in all .env files)

Validation:
    Jwts.parser()
        .verifyWith(secretKey)    ← recomputes HMAC, compares with token signature
        .build()
        .parseSignedClaims(token) ← throws JwtException if signature mismatch or expired
        .getPayload()             ← returns claims if valid
```

---

## 6. Authenticated Request Flow (Token Injection)

Every API call after login automatically carries the token. Here's how:

```
React component calls eventService.getOrganiserEvents()
        │
        ▼
Axios instance (eventApi) creates HTTP request
        │
        ▼
Request interceptor runs (registered ONCE at module load in event.ts)
        │
        if (_accessToken != null)
            request.headers.Authorization = "Bearer <token>"
        │
        ▼  GET /api/organiser/events
           Authorization: Bearer eyJhbGci...
        │
        ▼  Vite proxy → Event Service :8081
        │
        ▼  OrganiserEventController.getOrganizerEvents(authHeader)
        │
        ▼  jwtUtil.extractOrganiserId(authHeader)
        ├── strips "Bearer " prefix
        ├── Jwts.parser().verifyWith(secretKey).parseSignedClaims(token)
        ├── checks claims.get("role") == "ORGANISER"  → else UnauthorizedException → 401
        └── returns UUID.fromString(claims.getSubject())  → organiserId
        │
        ▼  eventService.getOrganizerEvents(organiserId, page, size)
        │
        ▼  200 OK  { content: [...], page, total }
```

### The `_accessToken` module variable pattern

```
// Registered ONCE when the module is first imported — never again
eventApi.interceptors.request.use((config) => {
    if (_accessToken) config.headers.Authorization = `Bearer ${_accessToken}`;
    return config;
});

// Updated by AuthContext on every login/logout — no re-registration needed
export const setEventApiToken = (token) => { _accessToken = token; }
```

This avoids the stacking bug where calling `interceptors.request.use()` inside `useEffect` would register a new interceptor on every re-render.

---

## 7. Route Guarding (ProtectedRoute)

```
User navigates to /my-bookings
        │
        ▼
ProtectedRoute checks:
        ├── isAuthenticated = false → <Navigate to="/login" />
        ├── isAuthenticated = true, role not in allowedRoles → <Navigate to="/" />
        └── isAuthenticated = true, role OK → render <MyBookingsView />
```

```
Route                        allowedRoles
─────────────────────────    ────────────
/events                      public (anyone)
/events/:eventId             public (anyone)
/my-bookings                 BUYER only
/orders/:orderId             BUYER only
/organiser/dashboard         ORGANISER only
/organiser/events            ORGANISER only
/organiser/events/create     ORGANISER only
/organiser/events/:id        ORGANISER only
```

---

## 8. BUYER Flow — Browse → Purchase

```
/events (EventListingView)
        │  GET /api/events?category=&city=&search=&page=0&size=12
        │  ← No auth required (public endpoint)
        ▼
Event cards displayed
        │
        ▼
User clicks event → /events/:eventId (EventDetailView)
        │  GET /api/events/:eventId
        │  ← No auth required
        ▼
Event detail + ticket tiers shown
        │
User selects tier + quantity → clicks "Buy Tickets"
        │
        ▼  POST /api/orders  { eventId, items: [{ tierId, quantity }] }
           Authorization: Bearer <token>
        │
        ▼  Vite proxy → Order Service :8082
        │
        ▼  OrderController.createOrder()
        ├── extractToken(authHeader) → strips "Bearer "
        ├── jwtUtil.validateBuyerRole(token) → asserts role == "BUYER" or 401
        └── jwtUtil.getBuyerIdFromToken(token) → extracts sub claim as UUID
        │
        ▼  OrderService.createOrder(request, buyerId)  [@Transactional]
        ├── eventServiceClient.getEvent(eventId)
        │       └── GET http://event-service:8080/api/events/:id  (internal HTTP call)
        ├── validate event.status == "PUBLISHED"
        ├── for each item:
        │       ├── find tier in event.tiers
        │       ├── check tier.status == "ACTIVE"
        │       ├── check remaining_qty >= requested quantity
        │       ├── check quantity <= tier.max_per_order
        │       └── calculate subtotal
        ├── totalAmount = sum of all subtotals
        ├── walletRepository.getBalance(buyerId)  ← cross-schema: reads auth.users
        ├── check balance >= totalAmount          → else InsufficientWalletBalanceException
        ├── walletRepository.debit(buyerId, totalAmount) ← UPDATE auth.users SET wallet_balance
        ├── ticketTierRepository.decrementQty(tierId, qty) ← UPDATE events.ticket_tiers
        ├── persist Order (status=CONFIRMED in mock mode) + OrderItems (snapshot tier/event data)
        └── auditService.logOrderCreated(...)
        │
        ▼  Response: { orderId, status: "CONFIRMED", paymentLinkId: null }
        │
        ▼
Frontend: navigate to /orders/:orderId (OrderConfirmationView)
        │  GET /api/orders/:orderId   Authorization: Bearer <token>
        ▼
Order confirmation page shown
```

---

## 9. BUYER Flow — Cancel Order

```
/my-bookings (MyBookingsView)
        │  GET /api/orders/my?page=0&size=20   Authorization: Bearer <token>
        ▼
Order list shown with Cancel button (if cancellable)
        │
User clicks Cancel → confirmation → POST /api/orders/:id/cancel
        │
        ▼  OrderService.cancelOrder(orderId, buyerId)  [@Transactional]
        ├── orderRepository.findById(orderId) → not found → 404
        ├── check order.buyerId == buyerId    → else 403 OrderAccessDeniedException
        ├── check order.status == CONFIRMED   → else cannot cancel
        ├── eventServiceClient.getEvent(...)
        ├── check event.eventDate > now + 24h → else OrderCancellationNotAllowedException
        ├── walletRepository.credit(buyerId, order.totalAmount) ← refund wallet
        ├── ticketTierRepository.restoreQty(tierId, qty)        ← restore inventory
        └── order.status = CANCELLED
        │
        ▼  Response: { message: "Order cancelled", refundedAmount: X }
        │
        ▼
Frontend: wallet balance updated via updateWalletBalance() in AuthContext
          UI refreshes booking list
```

---

## 10. ORGANISER Flow — Create and Publish Event

```
/organiser/events/create (OrganiserEventCreateView)
        │
User fills form: title, category, venue, date, tiers
        │
        ▼  POST /api/organiser/events   Authorization: Bearer <token>
        │
        ▼  OrganiserEventController.createEvent()
        ├── jwtUtil.extractOrganiserId(authHeader)
        │       ├── verify JWT signature
        │       ├── assert role == "ORGANISER"
        │       └── return sub claim as UUID (organiserId)
        └── eventService.createEvent(request, organiserId)
        │
        ▼  Event saved with status = DRAFT
        │
Organiser adds tiers:
        POST /api/organiser/events/:id/tiers   Authorization: Bearer <token>
        │
Organiser publishes:
        PATCH /api/organiser/events/:id/publish   Authorization: Bearer <token>
        ├── eventService.publishEvent(eventId, organiserId)
        ├── check at least one ACTIVE tier exists → else BusinessRuleViolationException
        └── event.status = PUBLISHED  ← now visible to buyers on /events
```

---

## 11. 401 Handling (Token Expired or Invalid)

```
Any API call with expired / invalid token
        │
        ▼  Backend returns HTTP 401
        │
        ▼
Axios response interceptor (registered once per service module)
        │
        if (error.response.status === 401)
            setAuthToken(null)       ← clear auth.ts module var
            setEventApiToken(null)   ← clear event.ts module var (in respective interceptor)
            setOrderApiToken(null)   ← clear order.ts module var
            window.location.href = "/login"
        │
        ▼
User lands on /login
localStorage still has old token — user logs in again → fresh token issued
```

---

## 12. Logout Flow

```
User clicks "Logout" button (in BuyerLayout or OrganiserLayout nav)
        │
        ▼
LogoutDialog opens (confirmation modal)
        │
User confirms → handleLogout()
        │
        ▼  POST /api/auth/logout   Authorization: Bearer <token>
        │
        ▼  AuthService.logout()
        └── auditService.logLogout("client", true)
            Response: { message: "Logged out successfully" }
        │
        (Note: the token is NOT invalidated on the server — it remains valid until
         its 24h expiry. This is accepted for a demo. Production would use a token
         denylist or short-lived tokens.)
        │
        ▼
LogoutDialog.onSuccess (or onError — client clears state regardless)
        │
        ▼  AuthContext.logout()
        ├── setUser(null)
        ├── setAccessToken(null)
        ├── setAuthToken(null)       ← auth.ts module var cleared
        ├── setEventApiToken(null)   ← event.ts module var cleared
        ├── setOrderApiToken(null)   ← order.ts module var cleared
        ├── localStorage.removeItem("user")
        └── localStorage.removeItem("accessToken")
        │
        ▼
navigate("/login")
        │
        ▼
AppRoutes: isAuthenticated = false → /login rendered
Future requests: interceptors read null token → no Authorization header sent
```

---

## 13. Complete Token Lifecycle Summary

```
REGISTER / LOGIN
    Auth Service generates JWT
    JWT_SECRET ──► HMAC-SHA256(header.payload) = signature
    Token = base64(header).base64(payload).signature
    Token sent to browser in response body

STORED IN
    React state (accessToken)
    localStorage("accessToken")
    Module-level _accessToken in auth.ts / event.ts / order.ts

INJECTED ON EVERY REQUEST
    Axios request interceptor: Authorization: Bearer <token>

VALIDATED BY BACKENDS
    Event Service  ─► JwtUtil.extractOrganiserId()  asserts ORGANISER role
    Order Service  ─► JwtUtil.validateBuyerRole()   asserts BUYER role
    (Each uses the same JWT_SECRET to verify signature independently)

EXPIRED / INVALID
    Backend returns 401
    Axios response interceptor clears token, redirects to /login

LOGOUT
    Client clears token from state + localStorage
    Server-side: no invalidation (demo — token technically valid until 24h expiry)
```
