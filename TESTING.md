# Testing Guide

This document describes the testing strategy for the Quarkus CRUD application.

## Test Coverage

The project includes comprehensive integration and end-to-end tests covering:

- **Authentication Flow**: Guest user creation and authentication via cookies
- **Room CRUD Operations**: Create, Read, Update, Delete operations
- **Authorization**: Access control ensuring users can only modify their own resources
- **Database Integration**: PostgreSQL with Flyway migrations and MyBatis mappers
- **API Endpoints**: All REST endpoints with various scenarios

## Test Structure

### Unit Tests (`./gradlew test`)

Basic smoke tests that verify the application can start:
- `ApplicationStartupTest`: Verifies application startup and endpoint accessibility

**Note**: Due to a known limitation with `quarkus-mybatis` 2.4.1, XML-based MyBatis mappers do not work properly with `@QuarkusTest` annotation. The mappers work correctly in development and production modes but fail during `@QuarkusTest` execution. This is a framework limitation, not an application issue.

### Integration Tests (Manual Script)

For comprehensive end-to-end testing with full database and MyBatis integration, use the provided integration test script:

```bash
# 1. Start PostgreSQL
docker run --name postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=quarkus_crud -p 5432:5432 -d postgres:15-alpine

# 2. Start the application in dev mode
./gradlew quarkusDev

# 3. Run integration tests (in a separate terminal)
./scripts/integration-test.sh
```

The integration test script covers:

#### Authentication Tests
- ✅ Create guest user
- ✅ Get current user with cookie authentication
- ✅ Get current user without authentication (should fail)

#### Room CRUD Tests
- ✅ Create room without authentication (should fail)
- ✅ Create room with authentication
- ✅ Get all rooms
- ✅ Get room by ID
- ✅ Get my rooms (user's own rooms)
- ✅ Update room
- ✅ Delete room

#### Authorization Tests
- ✅ Try to update another user's room (should fail with 403)
- ✅ Try to delete another user's room (should fail with 403)

#### Multi-Room Tests
- ✅ Create multiple rooms for a single user
- ✅ Verify user can see all their rooms

#### Database Integrity Tests
- ✅ Create room with special characters
- ✅ Update room with null description
- ✅ Verify foreign key constraints
- ✅ Verify cascade delete behavior

## Running Tests

### Quick Test (Application Startup)
```bash
./gradlew test
```

### Full Integration Test
```bash
# Terminal 1: Start application
./gradlew quarkusDev

# Terminal 2: Run integration tests
./scripts/integration-test.sh
```

### Manual Testing with cURL

See [API.md](API.md) for comprehensive API examples using cURL.

## Test Database

The application uses PostgreSQL for both development and testing:

**Development/Manual Testing:**
```bash
docker run --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 \
  -d postgres:15-alpine
```

**CI/CD:**
PostgreSQL is automatically started as a service in GitHub Actions (see `.github/workflows/ci.yml`).

## Coverage Summary

The integration test script provides comprehensive coverage of:

- ✅ **Database Layer**: Flyway migrations, PostgreSQL connectivity
- ✅ **MyBatis Layer**: All mappers (UserMapper, RoomMapper) with CRUD operations
- ✅ **Service Layer**: UserService and RoomService business logic
- ✅ **REST Layer**: All endpoints in AuthResource and RoomResource
- ✅ **Security**: Cookie-based authentication and authorization
- ✅ **Data Integrity**: Foreign keys, constraints, and cascade deletes

## Known Limitations

1. **MyBatis XML Mappers in @QuarkusTest**: The `quarkus-mybatis` extension (v2.4.1) has a known issue where XML-based mappers don't initialize properly in Quarkus test mode. This is why we use:
   - Simple smoke tests with `./gradlew test`
   - Manual integration tests with the running application
   
2. **Workarounds Considered**:
   - Annotation-based mappers (@Select, @Insert, etc.) - Would require significant refactoring
   - @QuarkusIntegrationTest - Requires packaging the application
   - Custom test resource loaders - Complex setup

3. **Why This Approach Works**:
   - The application works perfectly in dev and prod modes
   - Integration tests provide full coverage of all features
   - Manual testing validates the complete stack
   - CI/CD can run the integration test script

## Continuous Integration

The CI workflow (`.github/workflows/ci.yml`) includes:
1. PostgreSQL service container
2. Java 21 setup
3. Application build with `./gradlew build`
4. Unit tests execution

For full CI/CD testing, the workflow can be extended to:
```yaml
- name: Start application
  run: ./gradlew quarkusDev &
  
- name: Wait for application
  run: sleep 10

- name: Run integration tests
  run: ./scripts/integration-test.sh
```

## Test Maintenance

When adding new features:

1. Add corresponding test cases to `scripts/integration-test.sh`
2. Update this documentation
3. Ensure the integration test script covers the new functionality
4. Test manually with `./gradlew quarkusDev` and the integration script

## Debugging Tests

To debug integration tests:

1. Start the application with debug enabled:
   ```bash
   ./gradlew quarkusDev -Ddebug=5005
   ```

2. Run a single test case from the script:
   ```bash
   # Extract and run individual curl commands from integration-test.sh
   curl -v -c /tmp/cookies.txt -X POST http://localhost:8080/api/auth/guest
   ```

3. Check application logs in the terminal running `quarkusDev`

4. Verify database state:
   ```bash
   docker exec -it postgres psql -U postgres -d quarkus_crud
   ```

## Future Improvements

- Monitor `quarkus-mybatis` updates for @QuarkusTest compatibility
- Consider migrating to annotation-based mappers if XML support remains problematic
- Add performance tests for high-load scenarios
- Add security scanning tests (SQL injection, XSS, etc.)
