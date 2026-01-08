# Test Architecture and Parallel Execution

This document explains the test structure, parallel execution strategy, and how JVM and Native mode tests share code while running equivalently.

## Test Structure Overview

The test suite is organized to ensure JVM and Native mode tests are equivalent and share the same test logic:

```
src/
├── test/
│   ├── java/app/aoki/quarkuscrud/
│   │   ├── *IntegrationTest.java     # JVM integration tests
│   │   ├── ApplicationStartupTest.java
│   │   ├── service/
│   │   │   └── *ServiceTest.java     # Unit tests
│   │   └── support/
│   │       └── *Resource.java         # Test support classes
│   └── resources/
│       ├── application.properties      # Test configuration
│       └── junit-platform.properties   # JUnit configuration
└── native-test/
    └── java/app/aoki/quarkuscrud/
        └── *IT.java                    # Native integration tests (extend JVM tests)
```

## Test Equivalence Strategy

All Native mode integration tests extend their corresponding JVM test class, ensuring complete test equivalence:

```java
// JVM Test (src/test/java)
@QuarkusTest
public class AuthenticationIntegrationTest {
    @Test
    public void testCreateGuestUser() {
        // Test implementation
    }
}

// Native Test (src/native-test/java)
@QuarkusIntegrationTest
public class AuthenticationIntegrationIT extends AuthenticationIntegrationTest {
    // Inherits all tests from JVM version
    // Tests run against native binary
}
```

### Coverage

All integration tests have both JVM and Native variants:
- ApplicationStartupTest → ApplicationStartupIT
- AuthenticationIntegrationTest → AuthenticationIntegrationIT
- AuthorizationIntegrationTest → AuthorizationIntegrationIT
- DataIntegrityIntegrationTest → DataIntegrityIntegrationIT
- EventCrudIntegrationTest → EventCrudIntegrationIT
- EventUserDataIntegrationTest → EventUserDataIntegrationIT
- FriendshipIntegrationTest → FriendshipIntegrationIT
- InvitationCodeDebugTest → InvitationCodeDebugIT
- LlmIntegrationTest → LlmIntegrationIT
- OpenAiApiValidationTest → OpenAiApiValidationIT
- OpenApiContractTest → OpenApiContractIT
- ProfileCrudIntegrationTest → ProfileCrudIntegrationIT

## Parallel Execution Configuration

### Gradle Level Parallelization

Tests run in parallel at the Gradle level using `maxParallelForks`:

```gradle
test {
    // Run test classes in separate JVM processes
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
}

tasks.named('testNative') {
    // Same parallelization for native tests
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
}
```

This approach:
- Runs each test class in its own JVM process
- Provides complete isolation between test classes
- Prevents Quarkus application context conflicts
- Scales with available CPU cores

### JUnit Configuration

JUnit Platform parallel execution is **disabled** (`junit-platform.properties`):

```properties
junit.jupiter.execution.parallel.enabled=false
```

**Why disabled?** Quarkus integration tests (`@QuarkusTest`) share an application context per test class. Enabling JUnit's parallel execution within a single JVM can cause race conditions during application startup. Gradle's `maxParallelForks` provides better isolation by using separate processes.

### Random Port Assignment

Each test class gets a random HTTP port to avoid conflicts:

```properties
# application.properties (test resources)
quarkus.http.test-port=0
```

This allows multiple test processes to run simultaneously without port collisions.

## Running Tests

### JVM Mode Tests

```bash
# Run all tests
./gradlew test

# Run specific integration test
./gradlew test --tests "AuthenticationIntegrationTest"

# Run all integration tests
./gradlew test --tests "*IntegrationTest"

# Run unit tests only
./gradlew test --tests "*.service.*Test"
```

### Native Mode Tests

```bash
# Build native binary and run all native tests
./gradlew testNative

# Run specific native integration test
./gradlew testNative --tests "AuthenticationIntegrationIT"
```

### Continuous Testing

During development with `./gradlew quarkusDev`, continuous testing is enabled:
- Tests automatically re-run when code changes
- Press `r` to manually trigger tests
- Press `o` to toggle output
- Press `p` to pause continuous testing

## Test Quality Characteristics

### Isolation
- Each test class runs in its own JVM process (Gradle `maxParallelForks`)
- Each test gets its own PostgreSQL database (Testcontainers + Dev Services)
- Each test uses a random HTTP port (no conflicts)

### Equivalence
- Native tests inherit all test methods from JVM tests
- Same test logic, different runtime (JVM vs Native)
- Same database, same APIs, same assertions

### Performance
- Parallel execution reduces total test time
- Number of parallel processes adapts to available CPU cores
- Testcontainers reuse is supported (configure `~/.testcontainers.properties`)

### Reliability
- Process isolation prevents cross-test contamination
- Random ports eliminate port conflicts
- Dev Services ensures consistent database state

## Best Practices

### When Writing New Tests

1. **Create the JVM test first** in `src/test/java`:
   ```java
   @QuarkusTest
   public class MyFeatureIntegrationTest {
       @Test
       public void testFeature() {
           // Test implementation
       }
   }
   ```

2. **Create the Native test** in `src/native-test/java`:
   ```java
   @QuarkusIntegrationTest
   public class MyFeatureIntegrationIT extends MyFeatureIntegrationTest {
       // Inherits all tests
   }
   ```

3. **Run both modes** to ensure equivalence:
   ```bash
   ./gradlew test --tests "MyFeatureIntegrationTest"
   ./gradlew testNative --tests "MyFeatureIntegrationIT"
   ```

### Test Organization

- **Integration tests**: Use `@QuarkusTest`, test full application behavior
- **Unit tests**: Test individual services/components in isolation
- **Contract tests**: Use swagger-request-validator for OpenAPI compliance

### Debugging Tests

1. **Disable parallel execution** for debugging:
   ```bash
   ./gradlew test --max-workers=1 --tests "MyTest"
   ```

2. **View test reports**:
   ```bash
   open build/reports/tests/test/index.html
   ```

3. **Check test logs**:
   ```bash
   tail -f build/test-results/test/TEST-*.xml
   ```

## Performance Tips

### Testcontainers Reuse

Enable container reuse to speed up test execution:

```bash
# ~/.testcontainers.properties
testcontainers.reuse.enable=true
```

This reuses the PostgreSQL container across test runs instead of recreating it.

### Gradle Build Cache

Enable Gradle build cache for faster incremental builds:

```bash
./gradlew test --build-cache
```

### Skip Tests During Development

When iterating on code:

```bash
./gradlew build -x test
```

## Continuous Integration

CI pipelines should run both JVM and Native tests:

```yaml
# Example GitHub Actions workflow
jobs:
  test-jvm:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run JVM tests
        run: ./gradlew test

  test-native:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build and test native
        run: ./gradlew testNative
```

## Troubleshooting

### Port Conflicts

If you see `QuarkusBindException`, ensure `quarkus.http.test-port=0` is set in test `application.properties`.

### Database Conflicts

If tests fail with database errors, check that Dev Services is enabled:
```properties
quarkus.datasource.devservices.enabled=true
```

### Parallel Execution Issues

If tests fail only when run in parallel:
1. Check for shared mutable state between tests
2. Verify `@TestInstance(Lifecycle.PER_CLASS)` is used correctly
3. Consider test execution order with `@Order` annotations

### Native Build Failures

If native tests fail but JVM tests pass:
1. Check native reflection configuration
2. Verify all dependencies support native mode
3. Review GraalVM compatibility of third-party libraries

## Summary

This test architecture achieves:
- ✅ Complete equivalence between JVM and Native tests
- ✅ Shared test code (DRY principle)
- ✅ Parallel execution for faster feedback
- ✅ Process isolation for reliability
- ✅ Random ports to prevent conflicts
- ✅ Maintained test quality across both modes
