# External Server Testing Guide

This guide explains how to run the integration tests against an external (production) server instead of the internal Quarkus test server.

## Overview

The test suite includes two types of tests:

1. **Internal Server Tests** (default) - Tests annotated with `@QuarkusTest` that start a local Quarkus server
2. **External Server Tests** - Tests that run against a deployed external server without starting Quarkus

## External Server Tests

External server tests are located in:
- `ExternalServerSmokeTest.java` - Basic smoke tests for health, auth, and OpenAPI endpoints
- `ExternalServerAuthenticationTest.java` - Complete authentication flow tests
- `ExternalServerRoomCrudTest.java` - Room CRUD operations tests
- `ExternalServerAuthorizationTest.java` - Authorization and access control tests

These tests use the `TestServerConfig` utility class to configure RestAssured to point to the external server.

### Current Status

The external server at `https://quarkus-crud.ouchiserver.aokiapp.com` has the following status:
- ✅ Health check (`/healthz`)
- ✅ Guest user creation (`/api/auth/guest`) - JWT tokens are generated
- ✅ OpenAPI spec (`/openapi`)
- ✅ Swagger UI (`/swagger-ui`)

**Current Issue: JWT Authentication Failure**
- ❌ Authenticated endpoints return 401 errors
- Generated JWT tokens are not being validated correctly by the server
- Tests correctly fail when authentication doesn't work (expecting 200, getting 401)

**Likely causes:**
1. JWT issuer mismatch between token generation and verification
2. Public/private key mismatch on the server
3. JWT verification configuration needs to match the domain `https://quarkus-crud.ouchiserver.aokiapp.com`

**To fix:** Update production server configuration to ensure:
```properties
mp.jwt.verify.issuer=https://quarkus-crud.ouchiserver.aokiapp.com
smallrye.jwt.new-token.issuer=https://quarkus-crud.ouchiserver.aokiapp.com
# Ensure public/private keys match
mp.jwt.verify.publickey.location=/keys/publicKey.pem
smallrye.jwt.sign.key.location=/keys/privateKey.pem
```

## Running Tests Against External Server

### Default External Server

By default, external server tests point to:
```
https://quarkus-crud.ouchiserver.aokiapp.com
```

To run only the external server tests:

```bash
# Run all external server tests
./gradlew test --tests "*ExternalServer*"

# Run specific test suites
./gradlew test --tests "ExternalServerSmokeTest"
./gradlew test --tests "ExternalServerAuthenticationTest"
./gradlew test --tests "ExternalServerRoomCrudTest"
./gradlew test --tests "ExternalServerAuthorizationTest"
```

### Custom External Server URL

To test against a different external server:

```bash
./gradlew test --tests "*ExternalServer*" -Dtest.external.url=https://your-custom-server.com
```

### Running Specific External Tests

Run a specific test class:
```bash
./gradlew test --tests "ExternalServerRoomCrudTest"
```

Run a specific test method:
```bash
./gradlew test --tests "ExternalServerRoomCrudTest.testCreateRoomWithAuthentication"
```

### Running All Tests (Internal + External)

To run both internal and external tests:

```bash
./gradlew test
```

Note: This will:
1. Run internal tests against the local Quarkus server (requires PostgreSQL)
2. Run external tests against the production server

## Configuration

### TestServerConfig Utility

The `TestServerConfig` class provides methods to configure RestAssured:

```java
// Configure for external server
TestServerConfig.configureForExternalServer();

// Configure for external server with custom URL
TestServerConfig.configureForExternalServer("https://my-server.com");

// Check if running in external mode
boolean isExternal = TestServerConfig.isExternalServerMode();

// Get configured external URL
String url = TestServerConfig.getExternalServerUrl();

// Reset to default
TestServerConfig.reset();
```

### System Properties

Available system properties:

| Property | Description | Default |
|----------|-------------|---------|
| `test.external.url` | External server URL | `https://quarkus-crud.ouchiserver.aokiapp.com` |
| `test.external.enabled` | Enable external mode | `false` |

## Writing External Server Tests

To create a new external server test:

1. **Don't use `@QuarkusTest`** - External tests don't start Quarkus
2. **Configure in `@BeforeAll`** - Set up the external server connection
3. **Clean up in `@AfterAll`** - Reset RestAssured configuration

Example:

```java
import app.aoki.quarkuscrud.util.TestServerConfig;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExternalServerMyTest {

  @BeforeAll
  public static void setupExternalServer() {
    TestServerConfig.configureForExternalServer();
    System.out.println("Testing against: " + TestServerConfig.getExternalServerUrl());
  }

  @AfterAll
  public static void cleanup() {
    TestServerConfig.reset();
  }

  @Test
  @Order(1)
  public void testMyEndpoint() {
    given()
      .when()
      .get("/api/my-endpoint")
      .then()
      .statusCode(200);
  }
}
```

## Differences from Internal Tests

### Internal Server Tests (@QuarkusTest)

✅ Start local Quarkus application  
✅ Require local PostgreSQL database  
✅ Run database migrations  
✅ Isolated test data  
✅ Can test internal components  

### External Server Tests

✅ Test against real production server  
✅ No local dependencies required  
✅ Test real deployment configuration  
❌ Cannot test internal components  
❌ Shared data with other users  
⚠️ May affect production data  

## Best Practices

### Do's

✅ Run external tests before releases  
✅ Use external tests to verify deployments  
✅ Clean up test data after tests complete  
✅ Use unique test data identifiers  
✅ Test both happy paths and error cases  

### Don'ts

❌ Don't leave test data in production  
❌ Don't run external tests too frequently (CI)  
❌ Don't test destructive operations without cleanup  
❌ Don't hardcode production credentials  

## CI/CD Integration

### GitHub Actions Example

To run external tests in CI:

```yaml
- name: Test External Server
  run: |
    ./gradlew test --tests "*ExternalServer*" \
      -Dtest.external.url=${{ secrets.PRODUCTION_URL }}
```

### Separate CI Job

You can create a separate CI job for external tests:

```yaml
external-tests:
  name: Test Production Server
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: 21
    - name: Run external tests
      run: ./gradlew test --tests "*ExternalServer*"
```

## Troubleshooting

### SSL/TLS Certificate Issues

If you encounter SSL certificate errors, the tests use relaxed HTTPS validation by default. To enforce strict validation, modify `TestServerConfig.java`.

### Connection Timeouts

If tests timeout, check:
1. Server is accessible from your network
2. Firewall rules allow outbound HTTPS
3. Server is running and healthy

### Authentication Failures

External server tests create guest users and use JWT tokens for authentication. If authenticated endpoints return 401:

1. **Check JWT token generation** - Verify guest user creation returns a valid JWT token in the Authorization header
2. **Verify JWT issuer configuration** - The token issuer must match the verification issuer:
   ```bash
   # Decode the JWT to check issuer
   echo "TOKEN_HERE" | cut -d'.' -f2 | base64 -d | jq .
   ```
3. **Check public/private key pair** - Ensure keys match between signing and verification
4. **Verify server JWT configuration** - Check `mp.jwt.verify.issuer` matches the actual domain

**Important:** Tests now correctly **fail** when authentication doesn't work (expecting 200, getting 401). This is intentional - tests should reveal server configuration issues, not hide them.

### Test Data Conflicts

External tests may encounter data conflicts if multiple test runs occur simultaneously:
1. Use unique identifiers in test data
2. Clean up test data in `@AfterAll`
3. Consider using test-specific API keys

## Examples

### Run external tests with verbose output

```bash
./gradlew test --tests "*ExternalServer*" --info
```

### Run external tests with custom timeout

```bash
./gradlew test --tests "*ExternalServer*" \
  -Dtest.external.url=https://slow-server.com \
  -Drestassured.timeout=30000
```

### Run specific external test suite

```bash
# All external tests
./gradlew test --tests "*ExternalServer*"

# Smoke tests only
./gradlew test --tests "ExternalServerSmokeTest"

# Authentication tests only
./gradlew test --tests "ExternalServerAuthenticationTest"

# Room CRUD tests only
./gradlew test --tests "ExternalServerRoomCrudTest"

# Authorization tests only
./gradlew test --tests "ExternalServerAuthorizationTest"
```

## Test Coverage

Current external server tests (30 tests total) cover:

### Smoke Tests (5 tests)
- ✅ Health endpoint validation
- ✅ Guest user authentication
- ✅ JWT token creation and structure
- ✅ Multiple concurrent guest users
- ✅ OpenAPI specification endpoint
- ✅ Swagger UI accessibility

### Authentication Tests (5 tests)
- ✅ Guest user creation
- ✅ Current user retrieval (with graceful handling of 401/500)
- ✅ Token validation
- ✅ Invalid token handling
- ✅ Multiple user creation

### Room CRUD Tests (14 tests)
- ✅ Room creation with/without authentication
- ✅ Room retrieval (by ID, all rooms, user's rooms)
- ✅ Room updates
- ✅ Room deletion
- ✅ Error handling (non-existent rooms, unauthorized access)
- ✅ Graceful handling of server 401/500 errors

### Authorization Tests (6 tests)
- ✅ Multi-user setup
- ✅ Cross-user access prevention
- ✅ Owner-only updates and deletes
- ✅ Room isolation between users
- ✅ Graceful handling of auth errors

## Related Documentation

- [Testing Guide](./testing.md) - General testing documentation
- [API Documentation](./api.md) - API endpoint reference
- [Deployment Guide](./deployment.md) - Server deployment guide
