# Quarkus Dev UI Guide

This guide provides comprehensive information about using the Quarkus Dev UI for enhanced development experience.

## What is Quarkus Dev UI?

Quarkus Dev UI is a powerful web-based developer console that provides:
- Real-time application monitoring and control
- Interactive API testing
- Database management tools
- Configuration management
- Container orchestration (Dev Services)
- Continuous testing dashboard

## Accessing Dev UI

### Prerequisites
- Application running in dev mode: `./gradlew quarkusDev`
- Docker running (for Dev Services)

### URL
Open your browser to: **http://localhost:8080/q/dev-ui**

## Core Features

### 1. Configuration Management

**Location**: Dev UI → Configuration

**Features**:
- View all active configuration properties
- See configuration sources (application.properties, environment variables, defaults)
- Override properties at runtime
- Search and filter configuration
- Identify unused properties

**Example Uses**:
```
# Check database URL being used
Search for: "datasource.jdbc.url"

# Override log level temporarily
Override: "quarkus.log.category.\"app.aoki.quarkuscrud\".level" = "TRACE"
```

### 2. Dev Services Dashboard

**Location**: Dev UI → Dev Services

**What are Dev Services?**
Dev Services automatically starts required containers (like PostgreSQL) in dev mode, eliminating manual setup.

**Features**:
- View running Dev Services containers
- See container status and health
- Access connection strings and credentials
- View container logs
- Restart or stop containers
- Container reuse across sessions

**PostgreSQL Dev Service**:
- Automatically starts PostgreSQL 15 Alpine container
- Creates database and user
- Runs Flyway migrations
- Provides JDBC URL for connection
- Container persists between dev mode sessions (reuse enabled)

**Managing Containers**:
```bash
# View Dev Services containers
docker ps | grep testcontainers

# Access container logs via Dev UI
Dev UI → Dev Services → PostgreSQL → Logs

# Disable Dev Services if needed
./gradlew quarkusDev -Dquarkus.devservices.enabled=false
```

### 3. Swagger UI Integration

**Location**: Dev UI → SmallRye OpenAPI → Swagger UI

**Features**:
- Interactive API documentation
- Test endpoints with custom inputs
- View request/response schemas
- Authentication testing
- Export OpenAPI specification

**Testing Workflow**:
1. Navigate to Swagger UI
2. Expand endpoint (e.g., POST /api/authentication/guest)
3. Click "Try it out"
4. Modify request body if needed
5. Click "Execute"
6. View response, headers, and status code
7. Use the returned JWT token for authenticated requests

**Example: Create and Use Guest User**:
```
1. POST /api/authentication/guest
   Response: { "id": 1, "token": "eyJ..." }

2. Copy the token
3. Click "Authorize" button at top of Swagger UI
4. Paste token: "Bearer eyJ..."
5. Now authenticated requests will work:
   - POST /api/rooms
   - GET /api/rooms/me
   - etc.
```

### 4. Continuous Testing

**Location**: Dev UI → Continuous Testing

**Features**:
- Run tests automatically on code changes
- View test results in real-time
- Filter by test status (passed, failed, skipped)
- See test execution time
- View detailed failure messages
- Run specific tests on demand

**Usage**:
```bash
# Start dev mode with continuous testing
./gradlew quarkusDev --tests

# Or enable in Dev UI
Dev UI → Continuous Testing → Enable
```

**Test Filtering**:
- All tests
- Only failed tests
- Only passing tests
- Search by test name or class

### 5. Health Checks

**Location**: Dev UI → SmallRye Health

**Features**:
- View liveness probe status
- View readiness probe status
- View startup probe status
- See individual health check details
- Monitor health over time

**Health Check Endpoints**:
- `/q/health` - All health checks
- `/q/health/live` - Liveness probe
- `/q/health/ready` - Readiness probe
- `/q/health/started` - Startup probe

**Example Checks**:
- Database connection health
- Disk space
- Memory usage

### 6. Build Information

**Location**: Dev UI → Info

**Features**:
- Application version
- Build timestamp
- Git commit information (branch, commit hash)
- Java version
- Quarkus version
- Operating system information
- Dependency versions

### 7. Database Tools

**Location**: Dev UI → Extensions → Datasources

**Features**:
- View datasource configuration
- See connection pool statistics
- Test database connectivity
- View active connections

**Advanced Database Access**:
For SQL queries and schema inspection, access the PostgreSQL container directly:
```bash
# Find the Dev Services PostgreSQL container
docker ps | grep postgres

# Access psql
docker exec -it <container-name> psql -U quarkus -d default

# Run queries
\dt                    # List tables
SELECT * FROM users;   # Query data
\d users              # Describe table structure
```

## Dev Mode Keyboard Shortcuts

While `./gradlew quarkusDev` is running, use these terminal commands:

| Key | Action |
|-----|--------|
| `r` | Force restart the application |
| `h` | Display all available commands |
| `s` | Force restart with cache clearing |
| `e` | Edit command line arguments |
| `i` | Toggle instrumentation-based reload |
| `l` | Toggle live reload |
| `w` | Open Dev UI in default browser |
| `Ctrl+C` | Stop dev mode |

## Live Reload

Quarkus automatically detects and reloads changes to:
- Java source files (`.java`)
- Resource files (`.properties`, `.yaml`, `.xml`)
- Static content (HTML, CSS, JS)
- Configuration files

**How it works**:
1. Make changes to code
2. Save file
3. Quarkus detects change
4. Application recompiles and reloads
5. Changes are live in seconds

**No reload needed for**:
- Configuration property changes (runtime override via Dev UI)
- Static resources (when using quarkus.live-reload.instrumentation)

## Common Workflows

### Workflow 1: Developing a New API Endpoint

1. Define endpoint in OpenAPI spec (`src/main/resources/META-INF/openapi.yaml`)
2. Gradle automatically regenerates models
3. Implement endpoint in Java
4. Save file (Quarkus reloads automatically)
5. Test in Swagger UI (Dev UI → Swagger UI)
6. View logs in terminal or Dev UI
7. Iterate on implementation

### Workflow 2: Debugging Database Issues

1. Open Dev UI → Dev Services → PostgreSQL
2. Check container status and logs
3. Note JDBC URL and credentials
4. Use psql to inspect data:
   ```bash
   docker exec -it <container> psql -U quarkus -d default
   \dt                          # List tables
   SELECT * FROM rooms;         # Query data
   ```
5. Check Flyway migrations in DB:
   ```sql
   SELECT * FROM flyway_schema_history;
   ```
6. Verify configuration in Dev UI → Configuration

### Workflow 3: Testing Authentication

1. Open Swagger UI (Dev UI → SmallRye OpenAPI)
2. Test POST `/api/authentication/guest`
3. Copy token from response
4. Click "Authorize" button
5. Enter: `Bearer <token>`
6. Test protected endpoints (POST `/api/rooms`, etc.)
7. Verify behavior

### Workflow 4: Running Tests While Developing

1. Start continuous testing: `./gradlew quarkusDev --tests`
2. Open Dev UI → Continuous Testing
3. Make code changes
4. Watch tests run automatically
5. Filter to failed tests if any
6. Fix issues
7. Tests re-run and pass

### Workflow 5: Configuration Tuning

1. Open Dev UI → Configuration
2. Search for property (e.g., "log.level")
3. Override at runtime: `quarkus.log.level=DEBUG`
4. Test behavior
5. If good, add to `application-dev.properties`
6. Reset override in Dev UI

## Troubleshooting

### Dev UI Not Accessible

**Problem**: Cannot access http://localhost:8080/q/dev-ui

**Solutions**:
1. Ensure running in dev mode: `./gradlew quarkusDev` (not `./gradlew quarkusBuild`)
2. Check application started successfully (no errors in logs)
3. Verify port 8080 is not in use: `lsof -i :8080`
4. Check browser console for errors

### Dev Services Container Not Starting

**Problem**: PostgreSQL Dev Service fails to start

**Solutions**:
1. Ensure Docker is running: `docker ps`
2. Check Docker logs: `docker logs <container-name>`
3. Verify port 5432 is available
4. Check Docker has enough resources (memory, disk)
5. Try cleaning up old containers: `docker system prune`

### Live Reload Not Working

**Problem**: Code changes don't trigger reload

**Solutions**:
1. Check terminal output for errors
2. Verify file is saved
3. Try manual reload: Press `r` in terminal
4. Check IDE auto-save is enabled
5. Verify file is in source directory (not build directory)

### Continuous Testing Issues

**Problem**: Tests don't run automatically

**Solutions**:
1. Ensure started with `--tests` flag
2. Enable in Dev UI → Continuous Testing
3. Check test compilation errors
4. Verify test dependencies in build.gradle
5. Check Dev UI → Continuous Testing for errors

## Best Practices

### 1. Always Use Dev Mode for Development
```bash
# Good: Full dev experience
./gradlew quarkusDev

# Avoid: Missing dev features
./gradlew run
```

### 2. Leverage Dev Services
- Let Quarkus manage containers
- Don't manually start PostgreSQL for dev
- Use container reuse for faster startup

### 3. Use Swagger UI for API Testing
- Faster than curl or Postman for quick tests
- Automatically synced with OpenAPI spec
- Built-in authentication support

### 4. Monitor Continuous Testing
- Catch regressions immediately
- Faster feedback loop
- Focus on failed tests quickly

### 5. Override Config in Dev UI First
- Test config changes without file edits
- Quick experimentation
- Move to properties file once confirmed

## Advanced Features

### Custom Dev UI Cards

You can extend Dev UI with custom cards for your application. See Quarkus documentation for creating custom Dev UI extensions.

### Remote Dev Mode

Run dev mode on a remote server and access Dev UI:
```bash
# On remote server
./gradlew quarkusDev -Dquarkus.http.host=0.0.0.0

# Access from local machine
http://<remote-ip>:8080/q/dev-ui
```

**Security Warning**: Only use on trusted networks. Dev UI exposes sensitive information.

### Dev Services Configuration

Customize Dev Services in `application-dev.properties`:
```properties
# Use specific PostgreSQL version
quarkus.datasource.devservices.image-name=postgres:16-alpine

# Don't reuse containers
quarkus.datasource.devservices.reuse=false

# Custom database name
quarkus.datasource.devservices.db-name=my_custom_db
```

## Integration with Testing

Tests run with Dev UI access the same Dev Services containers, ensuring consistency:

```java
@QuarkusTest
public class MyTest {
    // This test uses the same PostgreSQL container
    // that Dev UI shows in Dev Services dashboard
}
```

## Resources

- [Quarkus Dev UI Documentation](https://quarkus.io/guides/dev-ui)
- [Quarkus Dev Services Guide](https://quarkus.io/guides/dev-services)
- [Quarkus Testing Guide](https://quarkus.io/guides/getting-started-testing)
- [Project Testing Documentation](testing.md)

## Summary

The Quarkus Dev UI transforms the development experience by:
- Eliminating manual setup (Dev Services)
- Providing real-time feedback (Live Reload, Continuous Testing)
- Enabling interactive debugging (Swagger UI, Configuration Editor)
- Centralizing development tools (Database, Health, Logs)

**Key Takeaway**: Start every development session with `./gradlew quarkusDev` and explore http://localhost:8080/q/dev-ui
