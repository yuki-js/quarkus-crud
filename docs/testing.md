# Testing Guide

## Test Strategy

The project uses a comprehensive testing strategy with multiple test types:

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions with real database
3. **Contract Tests**: Validate API responses against OpenAPI specification
4. **Application Startup Tests**: Verify application boots correctly

## Running Tests

### All Tests

```bash
./gradlew test
```

### Specific Test Class

```bash
./gradlew test --tests "app.aoki.quarkuscrud.RoomCrudIntegrationTest"
```

### Specific Test Method

```bash
./gradlew test --tests "app.aoki.quarkuscrud.RoomCrudIntegrationTest.testCreateRoom"
```

### With Detailed Output

```bash
./gradlew test --info
```

## Test Categories

### Unit Tests

Located in: `src/test/java/app/aoki/quarkuscrud/service/`

Examples:
- `UserServiceTest`: Tests UserService business logic
- `AuthenticationProviderTest`: Tests authentication provider enum

These tests use mocks and don't require external dependencies.

### Integration Tests

Located in: `src/test/java/app/aoki/quarkuscrud/`

Examples:
- `RoomCrudIntegrationTest`: Tests full CRUD operations on rooms
- `AuthenticationIntegrationTest`: Tests authentication flow
- `AuthorizationIntegrationTest`: Tests access control
- `DataIntegrityIntegrationTest`: Tests data validation and edge cases

These tests:
- Use `@QuarkusTest` annotation
- Start the application
- Connect to a test PostgreSQL database
- Use RestAssured for HTTP testing

### Contract Tests

- `OpenApiContractTest`: Validates API responses against OpenAPI spec

Uses Swagger Request Validator to ensure API compliance.

### Application Tests

- `ApplicationStartupTest`: Verifies application starts correctly

## Test Database

Integration tests require PostgreSQL. The tests use:

- **Host**: localhost
- **Port**: 5432
- **Database**: quarkus_crud
- **Username**: postgres
- **Password**: postgres

### Starting Test Database

Using Docker:
```bash
docker run -d --name quarkus-test-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 \
  postgres:15-alpine
```

### Database Cleanup

Tests are transactional and roll back changes automatically. However, you can manually reset:

```bash
docker exec -it quarkus-test-postgres psql -U postgres -d quarkus_crud -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

## Writing Tests

### Integration Test Example

```java
package app.aoki.quarkuscrud;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@QuarkusTest
public class MyIntegrationTest {

  @Test
  public void testEndpoint() {
    // Create a guest user first
    String cookie = RestAssured.given()
        .when()
        .post("/api/auth/guest")
        .then()
        .statusCode(200)
        .extract()
        .cookie("guest_token");

    // Test authenticated endpoint
    RestAssured.given()
        .cookie("guest_token", cookie)
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(200)
        .body("size()", is(0));
  }
}
```

### Unit Test Example

```java
package app.aoki.quarkuscrud.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MyServiceTest {

  @Inject
  MyService myService;

  @Test
  public void testBusinessLogic() {
    var result = myService.doSomething("input");
    assertEquals("expected", result);
  }
}
```

## Test Coverage

### Key Test Areas

✅ **Authentication**
- Guest user creation
- Token validation
- Cookie handling
- Authorization checks

✅ **CRUD Operations**
- Create rooms
- Read rooms (single and list)
- Update rooms
- Delete rooms

✅ **Authorization**
- Owner-only operations
- Multi-user scenarios
- Forbidden access attempts

✅ **Data Integrity**
- Special characters handling
- Unicode support
- Null values
- Edge cases
- Database constraints

✅ **Database Integration**
- PostgreSQL connectivity
- Flyway migrations
- MyBatis queries
- Transaction management

✅ **REST API**
- All endpoints
- Error responses
- Content types
- Status codes

✅ **OpenAPI Contract**
- Request validation
- Response validation
- Schema compliance

## Continuous Integration

Tests run automatically in CI on:
- Every push
- Every pull request

CI workflow includes:
1. Start PostgreSQL service
2. Generate OpenAPI models
3. Check code formatting
4. Run linting
5. Build and test
6. Verify database state

See `.github/workflows/ci.yml` for details.

## Test Configuration

Test configuration: `src/test/resources/application.properties`

Override for tests:
```properties
# Use separate test database
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus_crud_test

# Log SQL queries in tests
quarkus.hibernate-orm.log.sql=true
```

## Best Practices

### Do's

✅ Use descriptive test names
✅ Test both happy path and error cases
✅ Clean up test data (handled automatically)
✅ Use RestAssured for API testing
✅ Validate response structure and content
✅ Test authorization properly
✅ Include edge cases

### Don'ts

❌ Don't share state between tests
❌ Don't depend on test execution order
❌ Don't hardcode IDs (use generated values)
❌ Don't skip cleanup
❌ Don't test implementation details

## Debugging Tests

### Run Single Test with Debug

```bash
./gradlew test --tests "MyTest.testMethod" --debug-jvm
```

Then attach debugger to port 5005.

### View Test Reports

After running tests:
```bash
open build/reports/tests/test/index.html
```

### Verbose Logging

Add to test class:
```java
@QuarkusTest
@TestProfile(VerboseTestProfile.class)
public class MyTest {
  // tests
}
```

Create profile:
```java
public class VerboseTestProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("quarkus.log.level", "DEBUG");
  }
}
```

## Performance Testing

For load testing, use tools like:
- Apache JMeter
- Gatling
- k6

Example k6 script:
```javascript
import http from 'k6/http';

export default function () {
  // Create guest user
  let res = http.post('http://localhost:8080/api/auth/guest');
  let cookie = res.cookies['guest_token'][0].value;

  // Create room
  http.post('http://localhost:8080/api/rooms',
    JSON.stringify({ name: 'Test Room' }),
    {
      headers: { 
        'Content-Type': 'application/json',
        'Cookie': `guest_token=${cookie}`
      }
    }
  );
}
```

## End-to-End (E2E) Testing

E2E tests validate the complete application from an external client perspective, without JVM dependencies.

### Running E2E Tests

E2E tests are bash-based and use curl to test the REST API.

```bash
# Run against local instance
./gradlew e2eTest

# Run against custom URL
BASE_URL=https://quarkus-crud.ouchiserver.aokiapp.com ./gradlew e2eTest

# Run script directly
BASE_URL=http://localhost:8080 ./scripts/e2e.sh
```

### E2E Test Coverage

The E2E test suite (`scripts/e2e.sh`) covers:

✅ **Health Check**
- Service availability
- Health endpoint response

✅ **Authentication & Authorization**
- Guest user creation
- JWT token extraction and usage
- Authenticated vs unauthenticated requests
- Authorization (401, 403 errors)

✅ **Room CRUD Operations**
- Create rooms (authenticated)
- List all rooms (public)
- Get room by ID
- Update rooms (owner-only)
- Delete rooms (owner-only)
- Get my rooms (authenticated)

✅ **Error Handling**
- 400 Bad Request (validation)
- 401 Unauthorized (no auth)
- 403 Forbidden (insufficient permissions)
- 404 Not Found (non-existent resources)

✅ **Multi-User Scenarios**
- Cross-user authorization
- Owner-only operations enforcement

### Prerequisites for E2E Tests

- Running application (local or remote)
- `curl` command-line tool
- `sed`, `grep` (POSIX-compliant shell utilities)

### Local E2E Testing

```bash
# 1. Start PostgreSQL
docker run --name quarkus-crud-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 -d postgres:15-alpine

# 2. Start application in dev mode
./gradlew quarkusDev

# 3. In another terminal, run E2E tests
./gradlew e2eTest
```

### Production E2E Testing

Test against deployed production instance:

```bash
BASE_URL=https://quarkus-crud.ouchiserver.aokiapp.com ./gradlew e2eTest
```

**Note**: Production tests create and clean up test data, they don't modify existing data.

### E2E Test Structure

The E2E test script:
- Uses POSIX-compliant shell syntax
- Stores responses in temporary files
- Validates HTTP status codes
- Checks response content
- Cleans up test data
- Exits with code 0 (success) or 1 (failure)

### CI/CD Integration

E2E tests run in CI for native images:

- **JVM variants**: Health check only (fast feedback)
- **Native variants**: Full E2E test suite

See `.github/workflows/publish-jib.yml` for details.

## Troubleshooting

### Tests Fail Due to Database

Ensure PostgreSQL is running:
```bash
docker ps | grep postgres
```

### Port Conflicts

Change test port:
```properties
quarkus.http.test-port=8081
```

### Flaky Tests

Enable test retry:
```properties
quarkus.test.retry=3
```

### E2E Tests Fail

Check application is running:
```bash
curl http://localhost:8080/healthz
```

Check logs for errors:
```bash
# Dev mode logs
# or
docker logs <container-name>
```

Verify BASE_URL is correct:
```bash
echo $BASE_URL
```
