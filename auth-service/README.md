# Authentication Service

Production-ready authentication service for the Event Ticketing Platform. Provides secure user identity management with JWT-based authentication, BCrypt password hashing, and token rotation.

## Features

- **User Registration**: Self-service registration for BUYER and ORGANISER roles
- **User Login**: Email and password authentication with BCrypt hashing (strength 12)
- **Token Management**: JWT access tokens (15 min) and refresh tokens (7 days)
- **Token Refresh**: Automatic token rotation for enhanced security
- **Logout**: Secure refresh token revocation
- **Audit Logging**: Comprehensive audit trail with masked sensitive data
- **Health Checks**: Multiple health endpoints for container orchestration

## Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.5
- **Build Tool**: Maven 3.9.6
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **Security**: BCrypt password hashing, JWT tokens (JJWT 0.12.5)
- **Testing**: JUnit 5, H2 in-memory database

## Architecture

```
Controller → Service → Repository → Database
```

- **Controllers**: Handle HTTP requests, validate inputs, return responses
- **Services**: Contain business logic (AuthService, AuditService)
- **Repositories**: JPA repositories for data access
- **Global Exception Handler**: Centralized error handling with @ControllerAdvice

## Database Schema

### auth.users
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| full_name | VARCHAR(255) | NOT NULL |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'BUYER' |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

### auth.refresh_tokens
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | NOT NULL, FK to users(id) |
| token | TEXT | NOT NULL, UNIQUE |
| expires_at | TIMESTAMP | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

## API Endpoints

### Authentication Endpoints

#### POST /api/auth/register
Register a new user (BUYER or ORGANISER only).

**Request:**
```json
{
  "email": "user@example.com",
  "fullName": "John Doe",
  "password": "SecurePass123",
  "role": "BUYER"
}
```

**Response (201):**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "BUYER",
    "isActive": true,
    "createdAt": "2026-04-15T10:30:00Z"
  }
}
```

#### POST /api/auth/login
Authenticate existing user.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": { ... }
}
```

#### POST /api/auth/refresh
Refresh access token using refresh token (implements token rotation).

**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": { ... }
}
```

#### POST /api/auth/logout
Revoke refresh token (idempotent operation).

**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response (200):**
```json
{
  "message": "Logged out successfully"
}
```

### Health Endpoints

- `GET /health` - General health check
- `GET /health/ready` - Kubernetes readiness probe
- `GET /health/live` - Kubernetes liveness probe
- `GET /v1/auth/health` - Versioned health check

## Configuration

Environment variables (defined in `application.yml`):

| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_PORT | 8080 | Server port |
| DATABASE_URL | jdbc:postgresql://localhost:5432/eventplatform | PostgreSQL connection URL |
| DATABASE_USERNAME | postgres | Database username |
| DATABASE_PASSWORD | postgres | Database password |
| JWT_SECRET | (auto-generated) | JWT signing secret (min 256 bits) |
| JWT_ACCESS_EXPIRY | 900000 | Access token expiry (15 min in ms) |
| JWT_REFRESH_EXPIRY | 604800000 | Refresh token expiry (7 days in ms) |

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 16
- Maven 3.9.6 (or use wrapper)

### Steps

1. **Start PostgreSQL**:
```bash
docker run -d \
  --name auth-postgres \
  -e POSTGRES_DB=eventplatform \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

2. **Build the application**:
```bash
./mvnw clean package
```

3. **Run the application**:
```bash
./mvnw spring-boot:run
```

The service will be available at `http://localhost:8080`.

## Running with Docker Compose

```bash
docker-compose up --build
```

This starts:
- PostgreSQL database on port 5432
- Auth service on port 8080

## Testing

### Run all tests:
```bash
./mvnw test
```

### Run tests with coverage report:
```bash
./mvnw test jacoco:report
```

Coverage report will be available at: `target/site/jacoco/index.html`

### Coverage Thresholds:
- Instructions: ≥90%
- Branches: ≥90%
- Lines: ≥95%
- Methods: ≥95%
- Classes: 100%

## Security Features

- **BCrypt Password Hashing**: Strength 12
- **JWT Tokens**: HS256 algorithm with configurable expiry
- **Token Rotation**: Old refresh tokens deleted on refresh
- **Generic Error Messages**: Same message for non-existent email or wrong password (prevents enumeration)
- **ADMIN Role Protection**: ADMIN role cannot be self-assigned
- **Password Requirements**: Minimum 8 characters with at least one digit
- **Audit Logging**: All auth events logged with masked sensitive data
- **Trace ID**: Automatic trace ID injection via MDC for request tracking

## Validation Rules

### Registration:
- Email must be unique and valid format
- Password: minimum 8 characters with at least one digit
- Role: BUYER or ORGANISER only (ADMIN rejected)
- All fields required

### Login:
- Email and password required
- Email must exist and password must match

### Refresh:
- Refresh token must exist in database
- Refresh token must not be expired
- Old token deleted before new one issued (rotation)

### Logout:
- Idempotent: returns 200 even if token not found

## Error Responses

All errors follow standard format:

```json
{
  "errorCode": "INVALID_CREDENTIALS",
  "message": "Invalid email or password",
  "timestamp": "2026-04-21T12:00:00Z",
  "traceId": "123e4567-e89b-12d3-a456-426614174000"
}
```

### Error Codes:
- `DUPLICATE_EMAIL` (409) - Email already registered
- `INVALID_CREDENTIALS` (401) - Wrong email or password
- `INVALID_TOKEN` (401) - Expired or unknown refresh token
- `INVALID_ROLE` (400) - Invalid role or ADMIN self-assignment attempt
- `VALIDATION_ERROR` (400) - Request validation failed
- `INTERNAL_ERROR` (500) - Unexpected server error

## OpenAPI Specification

The complete OpenAPI 3.0 specification is available at:
```
swagger/auth-openapi.yaml
```

Import this file into Swagger UI or Postman for interactive API documentation.

## Project Structure

```
auth_service/
├── src/
│   ├── main/
│   │   ├── java/com/eventplatform/auth/
│   │   │   ├── controller/         # REST controllers
│   │   │   ├── service/            # Business logic
│   │   │   ├── repository/         # Data access
│   │   │   ├── entity/             # JPA entities
│   │   │   ├── dto/                # Data transfer objects
│   │   │   ├── enums/              # Enumerations
│   │   │   ├── exception/          # Custom exceptions
│   │   │   ├── config/             # Configuration classes
│   │   │   ├── util/               # Utilities (JWT)
│   │   │   └── AuthApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/       # Flyway migrations
│   └── test/                       # JUnit 5 tests
├── swagger/
│   └── auth-openapi.yaml
├── build.gradle
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Logging

- **Application Logs**: Standard SLF4J/Logback
- **Audit Logs**: Separate AUDIT logger
- **Trace ID**: Automatically injected via MDC
- **Log Pattern**: `%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] %-5level %logger{36} - %msg%n`

## Development Guidelines

- No Lombok - explicit getters/setters/constructors
- No Mockito - use real objects or H2 for testing
- Controller validation with `@Valid` - no duplication in services
- Business logic only in services
- Global exception handling in ControllerAdvice
- No manual trace ID appending - automatic via MDC
- Follow layered architecture: Controller → Service → Repository

## Production Readiness

✅ Zero compilation errors  
✅ Comprehensive test coverage (≥90%)  
✅ Database migrations with Flyway  
✅ Health check endpoints  
✅ Containerized with Docker  
✅ Audit logging with masked sensitive data  
✅ Centralized error handling  
✅ OpenAPI 3.0 specification  
✅ Security best practices (BCrypt, JWT, token rotation)  
✅ Environment-based configuration  

## License

Proprietary - Event Ticketing Platform
