# Development Guide

## Prerequisites

### Required Software

- **Java 21**: OpenJDK Temurin distribution recommended
- **Docker**: For running Dev Services (automatic PostgreSQL container management)
- **Gradle**: Build tool (wrapper included in project)

### Optional Tools

- **Node.js 22+**: For OpenAPI validation with Spectral CLI
- **kubectl**: For Kubernetes deployment
- **curl or httpie**: For API testing
- **PostgreSQL client**: For manual database inspection (psql)

## Project Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yuki-js/quarkus-crud.git
cd quarkus-crud
```

### 2. Run the Application in Dev Mode

**🎉 New: Automatic PostgreSQL with Dev Services!**

No need to manually start PostgreSQL! Quarkus Dev Services will automatically:
- Start a PostgreSQL 15 container
- Configure database connection
- Run Flyway migrations
- Keep the container running between sessions

Simply run:
```bash
./gradlew quarkusDev
```

The application will start on http://localhost:8080

### 3. Access the Quarkus Dev UI

**🎨 Enhanced Developer Experience with Dev UI**

Open your browser to: **http://localhost:8080/q/dev-ui**

The Dev UI provides:
- 📊 **Configuration Editor**: View and modify configuration properties
- 🗄️ **Database Tools**: Inspect PostgreSQL tables, run queries
- 📝 **Swagger UI**: Test API endpoints interactively
- 🔍 **Health Checks**: Monitor application health
- 📈 **Metrics**: View application metrics
- 🔐 **Security Info**: JWT configuration and authentication details
- 🐳 **Dev Services**: Manage the automatic PostgreSQL container

### Alternative: Manual PostgreSQL Setup (for production-like testing)

If you need to test with an external PostgreSQL instance:

```bash
docker run -d --name quarkus-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 \
  postgres:15-alpine
```

Then disable Dev Services:
```bash
./gradlew quarkusDev -Dquarkus.devservices.enabled=false
```

## Quarkus Dev UI Features

### Overview

The Quarkus Dev UI is your command center for development. Access it at **http://localhost:8080/q/dev-ui** when running in dev mode.

### Key Features

#### 1. Configuration Editor
- View all configuration properties
- See which values are from defaults vs. custom configuration
- Override properties at runtime without restarting

#### 2. Continuous Testing
- Run tests automatically on code changes
- View test results in real-time
- Filter and search test results

#### 3. OpenAPI / Swagger UI
- Interactive API documentation at `/q/dev-ui/io.quarkus.quarkus-smallrye-openapi/swagger-ui`
- Test endpoints directly from the browser
- View request/response schemas
- Try authentication flows

#### 4. Dev Services Dashboard
- See status of auto-started containers (PostgreSQL)
- View container logs
- Container connection details
- Restart or stop containers

#### 5. Database Tools
- View database schema
- Browse tables and data
- Run SQL queries
- Monitor Flyway migrations

#### 6. Health Checks
- View liveness, readiness, and startup probes
- See individual health check status
- Debug health check failures

#### 7. Build Info
- View build time, Git commit info
- Application version and dependencies
- JVM and Quarkus version information

### Dev Mode Keyboard Shortcuts

When running `./gradlew quarkusDev`, use these shortcuts in the terminal:

- **`r`**: Force restart the application
- **`h`**: Display help
- **`s`**: Force restart and clear caches
- **`e`**: Edit command line args
- **`w`**: Open Dev UI in browser

### Live Reload

Code changes are automatically detected and the application reloads:
- Java source files
- Configuration files (application.properties)
- Static resources
- OpenAPI specification

No need to restart manually!

### Testing in Dev Mode

Run continuous testing alongside dev mode:

```bash
./gradlew quarkusDev --tests
```

Or press `r` in the terminal and then `t` to toggle continuous testing.

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

### Using Dev UI for Debugging

The Dev UI provides excellent debugging capabilities:

1. **Configuration Issues**: Check the Configuration page to see all active properties
2. **Database Issues**: Use the Database Tools to inspect schema and data
3. **API Issues**: Test endpoints directly in Swagger UI with various inputs
4. **Health Issues**: Monitor health checks in real-time
5. **Container Issues**: View Dev Services logs and connection details

### Database Debugging

**Option 1: Using Dev UI** (Recommended)
- Open Dev UI at http://localhost:8080/q/dev-ui
- Navigate to Dev Services → PostgreSQL
- View connection details and logs
- Use database tools to run queries

**Option 2: Direct PostgreSQL Access**

When using Dev Services, find the container name:
```bash
docker ps | grep postgres
```

Access PostgreSQL:
```bash
# Dev Services container (name will vary)
docker exec -it <container-name> psql -U quarkus -d default

# Or if using manual PostgreSQL:
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
