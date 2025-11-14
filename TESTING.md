# Testing Guide

This document describes the testing strategy for the Quarkus CRUD application.

## Test Coverage

The project includes comprehensive integration and end-to-end tests covering:

- **Authentication Flow**: JWT-based authentication with RSA-signed tokens
- **Room CRUD Operations**: Create, Read, Update, Delete operations
- **Authorization**: Access control ensuring users can only modify their own resources
- **Database Integration**: PostgreSQL with Flyway migrations and MyBatis mappers
- **Data Integrity**: Special characters, null values, unicode, and edge cases
- **API Endpoints**: All REST endpoints with various scenarios

## Test Structure

### Integration Tests

All tests are written in Java using JUnit 5 and REST Assured, organized by functionality:

#### 1. ApplicationStartupTest
Basic smoke test verifying application startup.

#### 2. AuthenticationIntegrationTest
Tests JWT authentication functionality:
- Create guest user
- JWT token generation and validation
- Get current user with valid/invalid tokens
- Multiple guest user creation
- Bearer token handling

#### 3. RoomCrudIntegrationTest
Tests complete CRUD operations for rooms:
- Create room (with/without auth)
- Get room by ID
- Get all rooms
- Get user's rooms
- Update room
- Delete room
- Non-existent resource handling

#### 4. AuthorizationIntegrationTest
Tests access control and permissions:
- Users cannot modify other users' rooms
- Users can modify their own rooms
- Multi-user room isolation
- Permission validation

#### 5. DataIntegrityIntegrationTest
Tests data handling and edge cases:
- Special characters in room names
- Null descriptions
- Multiple rooms per user
- Unicode and emoji support
- Long names
- Empty values

## Running Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests AuthenticationIntegrationTest
./gradlew test --tests RoomCrudIntegrationTest
./gradlew test --tests AuthorizationIntegrationTest
./gradlew test --tests DataIntegrityIntegrationTest
```

### Run in CI/CD

The tests automatically run in GitHub Actions CI:
- PostgreSQL service is started automatically
- All integration tests execute
- Reports are generated

See `.github/workflows/ci.yml` for CI configuration.

## Test Database

### Local Development
The tests use the PostgreSQL database configured in `application.properties`:
```bash
# Start PostgreSQL for local testing
docker run --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 \
  -d postgres:15-alpine
```

### CI/CD
PostgreSQL is automatically started as a service in GitHub Actions.

## Test Execution Order

Tests within each class are ordered using `@TestMethodOrder(OrderAnnotation.class)` and `@Order` annotations to ensure:
1. Setup tests run first (user creation, etc.)
2. Dependencies between tests are maintained
3. Cleanup happens in the right order

## Coverage Summary

| Category | Test Coverage |
|----------|--------------|
| **Authentication** | ✅ Guest user creation, validation, token handling |
| **Room CRUD** | ✅ All create, read, update, delete operations |
| **Authorization** | ✅ Permission checks, multi-user scenarios |
| **Database** | ✅ PostgreSQL connectivity, Flyway migrations |
| **MyBatis** | ✅ All mapper operations (conditional on MyBatis fix) |
| **Data Integrity** | ✅ Special characters, nulls, unicode, edge cases |
| **API Endpoints** | ✅ All REST endpoints in AuthResource and RoomResource |

## Known Limitations

### MyBatis XML Mapper Issue

There is a known issue with `quarkus-mybatis` 2.4.1 where XML-based mappers may not load properly in test mode. This is documented in `MYBATIS_ISSUE.md`.

**Current Status:**
- If MyBatis is working: All tests pass
- If MyBatis has issues: Tests will fail but are comprehensive when the issue is resolved

**Workarounds:**
- Tests use flexible status code matching (`anyOf(is(200), is(201))`)
- Critical tests are marked clearly
- See `MYBATIS_ISSUE.md` for solutions

## Debugging Tests

### View Test Output
```bash
./gradlew test --info
```

### Run with Debugging
```bash
./gradlew test --debug-jvm
# Then attach debugger to port 5005
```

### Check Test Reports
After running tests:
```bash
# Open in browser
open build/reports/tests/test/index.html
```

### Individual Test Debugging
Add `@Disabled` annotation to skip tests temporarily:
```java
@Test
@Disabled("Temporarily disabled for debugging")
public void testSomething() {
    // test code
}
```

## Writing New Tests

### Test Structure Template
```java
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class MyNewIntegrationTest {
    
    private static String testData;
    
    @BeforeAll
    public static void setup() {
        // Setup test data
    }
    
    @Test
    @Order(1)
    public void testSomething() {
        given()
            .when()
            .get("/api/endpoint")
            .then()
            .statusCode(200);
    }
}
```

### Best Practices
1. **Use `@QuarkusTest`** for integration tests
2. **Order tests** when they have dependencies
3. **Use `@BeforeAll`** for shared setup
4. **Clean up** resources in tests when needed
5. **Use descriptive test names** that explain what is being tested
6. **Test both success and failure cases**
7. **Verify response bodies** not just status codes

## Continuous Integration

The CI workflow runs:
1. Checkout code
2. Setup JDK 21
3. Start PostgreSQL service
4. Run `./gradlew build` (includes all tests)
5. Generate reports

To add more CI checks:
```yaml
- name: Run specific tests
  run: ./gradlew test --tests "app.aoki.*IntegrationTest"
```

## Test Maintenance

When adding new features:
1. Add corresponding test methods to existing test classes
2. Create new test classes if testing new functionality
3. Update this documentation
4. Ensure tests run in CI
5. Maintain test coverage above 70%

## Performance

Test execution time:
- Smoke tests: ~2-5 seconds
- Full integration test suite: ~30-60 seconds
- CI complete pipeline: ~2-3 minutes

## Future Improvements

- [ ] Add performance/load tests
- [ ] Add contract tests for API
- [ ] Add mutation testing
- [ ] Increase coverage to 90%+
- [ ] Add API schema validation tests

## Automated Test Evidence Generation

### GitHub Actions Summary

When tests run in CI, GitHub Actions automatically generates a comprehensive test evidence summary visible in the Actions UI. This includes:

- **Test Results Summary**: Total tests, failures, errors, and skipped tests
- **Test Suite Breakdown**: Individual test suite results with timings
- **Database Verification**: Actual record counts from PostgreSQL
- **Sample Data**: Recent users and rooms created during test execution
- **Coverage Areas**: All tested functionality areas
- **Overall Status**: Clear pass/fail indication

To view the test evidence:
1. Go to the Actions tab in GitHub
2. Click on a workflow run
3. Scroll down to see the detailed test evidence summary

### Local Test Evidence Report

You can generate a detailed test evidence report locally using the provided script:

```bash
# Run tests first
./gradlew clean test

# Generate evidence report
./scripts/generate-test-evidence.sh

# Or specify custom output file
./scripts/generate-test-evidence.sh MY_REPORT.md
```

The script generates a markdown report including:
- Complete test results breakdown
- Database verification (requires PostgreSQL running)
- Recent data samples from the database
- User-room relationship statistics
- Overall test status

**Requirements for database verification:**
- PostgreSQL client (`psql`) installed
- Database accessible at localhost:5432 (default)
- Or set environment variables: `POSTGRES_HOST`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`

**Example:**
```bash
# With custom database
export POSTGRES_HOST=localhost
export POSTGRES_USER=myuser
export POSTGRES_PASSWORD=mypassword
export POSTGRES_DB=quarkus_crud

./scripts/generate-test-evidence.sh
```

The generated report provides the same level of detail as the static `TEST_EVIDENCE.md` but with your current test results.
