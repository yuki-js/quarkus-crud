# Development Guide

## Prerequisites

### Required Software

- **Java 21**: OpenJDK Temurin distribution recommended
- **PostgreSQL 15+**: Database server
- **Gradle**: Build tool (wrapper included in project)
- **Docker**: For running PostgreSQL and building containers (optional)

### Optional Tools

- **Node.js 22+**: For OpenAPI validation with Spectral CLI
- **kubectl**: For Kubernetes deployment
- **curl or httpie**: For API testing

## Project Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yuki-js/quarkus-crud.git
cd quarkus-crud
```

### 2. Start PostgreSQL

Using Docker:
```bash
docker run -d --name quarkus-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 \
  postgres:15-alpine
```

Or use an existing PostgreSQL installation and create the database:
```sql
CREATE DATABASE quarkus_crud;
```

### 3. Run the Application

Development mode with live reload:
```bash
./gradlew quarkusDev
```

The application will start on http://localhost:8080

## Development Workflow

### Code Formatting

The project uses Google Java Format for consistent code style.

Check formatting:
```bash
./gradlew spotlessCheck
```

Apply formatting:
```bash
./gradlew spotlessApply
```

### Code Quality (Linting)

The project uses Checkstyle for code quality checks.

Run Checkstyle:
```bash
./gradlew checkstyleMain checkstyleTest
```

### OpenAPI Code Generation

API models and interfaces are generated from the OpenAPI specification.

Generate models:
```bash
./gradlew generateOpenApiModels
```

This is automatically run before compilation.

### Database Migrations

Flyway handles database schema migrations automatically on application startup.

Migration files are located in: `src/main/resources/db/migration/`

### Building

Build the application:
```bash
./gradlew build
```

This will:
1. Generate OpenAPI models
2. Compile Java code
3. Run tests
4. Package the application

### Running Tests

Run all tests:
```bash
./gradlew test
```

Run specific test:
```bash
./gradlew test --tests "app.aoki.quarkuscrud.RoomCrudIntegrationTest"
```

### Creating a Native Executable

Build native executable:
```bash
./gradlew build -Dquarkus.package.type=native
```

Requirements:
- GraalVM or Mandrel installation
- Native image prerequisites for your OS

## Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/app/aoki/quarkuscrud/
│   │   │   ├── entity/              # Domain entities (User, Room)
│   │   │   ├── mapper/              # MyBatis database mappers
│   │   │   ├── resource/            # JAX-RS REST resources
│   │   │   ├── service/             # Business logic services
│   │   │   ├── filter/              # Authentication filters
│   │   │   └── support/             # Exception mappers, health checks
│   │   └── resources/
│   │       ├── db/migration/        # Flyway SQL migrations
│   │       ├── META-INF/
│   │       │   └── openapi.yaml     # OpenAPI specification
│   │       ├── application.properties
│   │       └── keys/                # JWT signing keys
│   └── test/
│       ├── java/app/aoki/quarkuscrud/  # Test classes
│       └── resources/               # Test resources
├── build.gradle                     # Build configuration
├── gradle.properties                # Gradle properties
├── settings.gradle                  # Gradle settings
├── .spectral.yaml                   # OpenAPI linting rules
├── config/checkstyle/               # Checkstyle configuration
├── manifests/                       # Kubernetes manifests
└── docs/                           # Documentation
```

## Configuration

### Application Configuration

Main configuration file: `src/main/resources/application.properties`

Key settings:
- Database connection
- JWT configuration
- Quarkus settings
- Logging levels

### Environment Variables

Override configuration with environment variables:

```bash
# Database
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/quarkus_crud
export QUARKUS_DATASOURCE_USERNAME=postgres
export QUARKUS_DATASOURCE_PASSWORD=postgres

# JWT
export SMALLRYE_JWT_SIGN_KEY_LOCATION=/path/to/private-key.pem
export MP_JWT_VERIFY_PUBLICKEY_LOCATION=/path/to/public-key.pem
```

## IDE Setup

### IntelliJ IDEA

1. Import as Gradle project
2. Enable annotation processing
3. Install Google Java Format plugin
4. Configure code style to use Google Java Format

### VS Code

1. Install Java Extension Pack
2. Install Quarkus extension
3. Configure formatter to use Google Java Format

## Debugging

### Debug Mode

Start in debug mode:
```bash
./gradlew quarkusDev --debug-jvm
```

Connect debugger to port 5005.

### Database Debugging

Access PostgreSQL:
```bash
docker exec -it quarkus-postgres psql -U postgres -d quarkus_crud
```

View tables:
```sql
\dt
```

Query data:
```sql
SELECT * FROM users;
SELECT * FROM rooms;
```

## Container Build

### JVM Container

Build JVM container:
```bash
./gradlew build jibDockerBuild
```

### Native Container

Build native container:
```bash
./gradlew build -Dquarkus.package.type=native jibDockerBuild -PnativeBuild=true
```

Run container:
```bash
docker run -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/quarkus_crud \
  -e QUARKUS_DATASOURCE_USERNAME=postgres \
  -e QUARKUS_DATASOURCE_PASSWORD=postgres \
  ghcr.io/yuki-js/quarkus-crud:0.0.1
```

## Troubleshooting

### Build Issues

Clear Gradle cache:
```bash
./gradlew clean
rm -rf .gradle build
```

### Database Connection Issues

Verify PostgreSQL is running:
```bash
docker ps | grep postgres
```

Test connection:
```bash
psql -h localhost -U postgres -d quarkus_crud
```

### Port Already in Use

Change port in `application.properties`:
```properties
quarkus.http.port=8081
```

## Contributing

1. Create a feature branch
2. Make changes
3. Run tests and checks:
   ```bash
   ./gradlew spotlessApply
   ./gradlew checkstyleMain checkstyleTest
   ./gradlew test
   ```
4. Commit changes
5. Push and create a pull request
