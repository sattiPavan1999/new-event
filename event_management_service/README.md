# Event Management Service

Event Management Service enables event organizers to create, configure, and manage events through their complete lifecycle while providing public users the ability to discover and browse published events.

## Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.x
- **Build Tool**: Maven 3.9.6
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **Testing**: JUnit 5 + Spring Boot Test
- **Container**: Docker + Docker Compose

## Features

### Admin Features (Organizers)
- Create events in DRAFT mode
- Configure multiple ticket tiers per event (max 10 tiers)
- Edit event details with status-based restrictions
- Publish events to make them publicly visible
- Cancel events to prevent further sales
- View sales summary reports with per-tier metrics

### Public Features
- Browse published events with filtering (category, city, search)
- View detailed event information with venue and ticket tiers
- Paginated results for performance

## Architecture

The application follows a layered architecture:
- **Controller Layer**: REST API endpoints
- **Service Layer**: Business logic
- **Repository Layer**: Data access (JPA)
- **Entity Layer**: Database models
- **DTO Layer**: Request/Response objects
- **Exception Layer**: Centralized error handling

## Database Schema

### Tables
- `events.venues`: Venue information
- `events.events`: Event details with status (DRAFT, PUBLISHED, CANCELLED)
- `events.ticket_tiers`: Ticket tier configuration with pricing and quantities

### Key Relationships
- Event → Venue (Many-to-One)
- Event → TicketTiers (One-to-Many with CASCADE delete)

## API Endpoints

### Admin Endpoints (Authentication Required)
- `POST /api/admin/events` - Create event
- `PUT /api/admin/events/{id}` - Update event
- `PATCH /api/admin/events/{id}/publish` - Publish event
- `PATCH /api/admin/events/{id}/cancel` - Cancel event
- `POST /api/admin/events/{id}/tiers` - Add ticket tier
- `PUT /api/admin/events/{id}/tiers/{tierId}` - Update tier
- `DELETE /api/admin/events/{id}/tiers/{tierId}` - Delete tier
- `GET /api/admin/events/{id}/summary` - View sales summary

### Public Endpoints (No Authentication)
- `GET /api/events` - Browse events (with filters)
- `GET /api/events/{id}` - View event detail

### Health Endpoints
- `GET /health/live` - Liveness probe
- `GET /health/ready` - Readiness probe
- `GET /v1/event-management/health` - Service health

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+ (or use included Maven wrapper)
- Docker and Docker Compose (for containerized deployment)
- PostgreSQL 16 (for local development)

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd event_management_service
   ```

2. **Build the application**
   ```bash
   ./mvnw clean package
   ```

3. **Run with local PostgreSQL**
   ```bash
   # Ensure PostgreSQL is running on localhost:5432
   # Database: eventdb, User: postgres, Password: postgres
   
   ./mvnw spring-boot:run
   ```

4. **Access the application**
   - Application: http://localhost:8080
   - Health: http://localhost:8080/health/live
   - API Docs: See swagger/event-management-openapi.yaml

### Docker Deployment

1. **Build and run with Docker Compose**
   ```bash
   docker-compose up --build
   ```

2. **Access the application**
   - Application: http://localhost:8080
   - PostgreSQL: localhost:5432

3. **Stop the services**
   ```bash
   docker-compose down
   ```

4. **Clean up volumes**
   ```bash
   docker-compose down -v
   ```

## Configuration

Configuration is externalized via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Application port |
| `DATABASE_URL` | jdbc:postgresql://localhost:5432/eventdb | Database JDBC URL |
| `DATABASE_USERNAME` | postgres | Database username |
| `DATABASE_PASSWORD` | postgres | Database password |
| `DATABASE_POOL_SIZE` | 10 | Connection pool size |

## Business Rules

1. **Event Creation**: Events are created in DRAFT status by default
2. **Tier Limit**: Maximum 10 tiers allowed per event
3. **Publishing**: Event must have at least one ACTIVE tier before publishing
4. **Status Restrictions**: 
   - Event date and venue can only be edited in DRAFT status
   - Title, description, category, and banner can be edited in any status
5. **Tier Modifications**:
   - Price and quantity cannot be changed if confirmed orders exist
   - Name and description can always be updated
6. **Tier Deletion**: Blocked if any orders reference the tier
7. **Public Visibility**: Only PUBLISHED events with future dates are visible to public users

## Error Handling

All errors follow a standard format:
```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Event not found with id: ...",
  "timestamp": "2026-04-21T10:00:00Z",
  "traceId": "abc123-def456-ghi789"
}
```

### HTTP Status Codes
- `200 OK` - Successful GET/PUT/PATCH
- `201 Created` - Successful POST
- `204 No Content` - Successful DELETE
- `400 Bad Request` - Validation error or business rule violation
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - System error

## Testing

Run all tests:
```bash
./mvnw test
```

Run with coverage (using JaCoCo):
```bash
./mvnw test jacoco:report
```

Coverage targets:
- Instructions ≥ 90%
- Branches ≥ 90%
- Lines ≥ 95%
- Methods ≥ 95%
- Classes 100%

## Audit Logging

The application includes structured audit logging with:
- Automatic trace ID capture via MDC
- Sensitive data masking (IDs, names, financial info)
- Event tracking for all CRUD operations
- Separate AuditService component

## OpenAPI Specification

Complete API documentation is available in:
- File: `swagger/event-management-openapi.yaml`
- Format: OpenAPI 3.0.3
- Usage: Import into Swagger UI or Redoc

## Security Considerations

- Authentication via `X-User-Id` header (JWT token integration expected)
- Role-based access control (ORGANISER, ADMIN)
- Ownership validation for event modifications
- Input validation using Jakarta Validation
- SQL injection prevention via JPA/Hibernate
- Centralized exception handling to mask internal details

## Production Readiness

- ✅ Health check endpoints for Kubernetes
- ✅ Graceful shutdown support
- ✅ Connection pooling
- ✅ Database migration with Flyway
- ✅ Multi-stage Docker build
- ✅ Non-root container user
- ✅ Structured logging with MDC
- ✅ Environment-specific configuration
- ✅ Comprehensive error handling

## Support

For issues or questions, please contact the development team or refer to the OpenAPI specification for detailed API documentation.
