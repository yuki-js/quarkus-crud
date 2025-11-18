# Quarkus CRUD Application

A modern, cloud-native CRUD application built with Quarkus, featuring guest authentication, real-time updates via Server-Sent Events, and full OpenAPI specification.

## Overview

This application provides a RESTful API for managing rooms with the following key features:

- **Guest Authentication**: Anonymous user authentication using JWT tokens stored in HTTP-only cookies
- **CRUD Operations**: Create, read, update, and delete rooms
- **Real-time Updates**: Live room updates via Server-Sent Events (SSE)
- **OpenAPI Specification**: Complete API documentation with contract validation
- **Cloud-Native**: Kubernetes-ready with health checks and observability

## Technology Stack

- **Framework**: Quarkus 3.x
- **Language**: Java 21
- **Database**: PostgreSQL 15
- **ORM**: MyBatis
- **Migrations**: Flyway
- **Authentication**: JWT (SmallRye JWT)
- **API Documentation**: OpenAPI 3.0 + Swagger UI
- **Build Tool**: Gradle
- **Container**: Docker + Jib

## Quick Start

### Prerequisites

- Java 21 (Temurin distribution recommended)
- Docker (for Dev Services - automatic PostgreSQL container)
- Gradle (wrapper included)

### Running Locally with Dev UI

**🎉 No manual PostgreSQL setup required!**

1. Start the application in dev mode:
```bash
./gradlew quarkusDev
```

Quarkus automatically:
- Starts a PostgreSQL container via Dev Services
- Runs database migrations
- Enables live reload

2. Access the application:
- **Dev UI**: http://localhost:8080/q/dev-ui (⭐ Start here!)
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/q/swagger-ui
- Health Check: http://localhost:8080/healthz

### Alternative: Manual PostgreSQL Setup

For production-like testing:
```bash
# Start PostgreSQL
docker run -d --name quarkus-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 \
  postgres:15-alpine

# Disable Dev Services
./gradlew quarkusDev -Dquarkus.devservices.enabled=false
```

## Documentation

- [Dev UI Guide](./dev-ui-guide.md) - **⭐ NEW!** Comprehensive guide to Quarkus Dev UI features
- [API Documentation](./api.md) - Detailed API endpoints and usage
- [Development Guide](./development.md) - Setup and development workflow
- [Testing Guide](./testing.md) - Testing strategy and running tests
- [Deployment Guide](./deployment.md) - Kubernetes deployment instructions

## Project Structure

```
src/
├── main/
│   ├── java/app/aoki/quarkuscrud/
│   │   ├── entity/          # Domain entities
│   │   ├── mapper/          # MyBatis mappers
│   │   ├── resource/        # REST resources
│   │   ├── service/         # Business logic
│   │   ├── filter/          # Authentication filters
│   │   └── support/         # Exception mappers, health checks
│   └── resources/
│       ├── db/migration/    # Flyway migrations
│       └── META-INF/        # OpenAPI spec, config
└── test/
    └── java/                # Integration and unit tests
```

## License

Apache 2.0
