# E2E Testing Guide

This guide describes how to run End-to-End (E2E) tests for the Quarkus CRUD API, both locally and against production.

## Overview

The E2E testing infrastructure consists of two complementary test suites:

1. **Bash/curl tests** (`scripts/e2e.sh`) - Lightweight, fast tests using only curl and shell
2. **TypeScript tests** (`e2e/`) - Comprehensive tests using OpenAPI-generated TypeScript client

Both test suites use the `BASE_URL` environment variable to target different environments (local or production).

## Prerequisites

### For Local Testing

- **Docker** - For running PostgreSQL and native container images
- **Java 21** with GraalVM - For building native executables
- **Gradle** - Included via wrapper (`./gradlew`)
- **curl** - For bash-based E2E tests
- **Node.js 18+** and **npm** - For TypeScript E2E tests
- **wget** - For downloading OpenAPI Generator CLI

### For Production Testing

- **curl** - For bash-based E2E tests
- **Node.js 18+** and **npm** - For TypeScript E2E tests
- **wget** - For downloading OpenAPI Generator CLI

## Local Testing (Full Flow)

### Step 1: Start PostgreSQL

```bash
docker run --name quarkus-crud-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 \
  -d postgres:15-alpine

# Wait for PostgreSQL to be ready
until docker exec quarkus-crud-postgres pg_isready -U postgres; do 
  sleep 1
done
```

### Step 2: Build Native Executable

```bash
./gradlew build -Dquarkus.package.type=native --no-daemon
```

This may take 5-10 minutes depending on your system.

### Step 3: Prepare Native Runner for Docker

```bash
mkdir -p build/jib-native
NATIVE_RUNNER=$(find build -name '*-runner' -type f ! -path "*/quarkus-app/*" | head -n 1)
cp "$NATIVE_RUNNER" build/jib-native/quarkus-run
chmod +x build/jib-native/quarkus-run
```

### Step 4: Build Local Docker Image

```bash
cat > Dockerfile.native <<'EOF'
FROM debian:bookworm-slim
COPY build/jib-native/quarkus-run /quarkus-run
RUN chmod +x /quarkus-run
EXPOSE 8080
ENTRYPOINT ["/quarkus-run"]
EOF

docker build -t local/quarkus-crud-native:latest -f Dockerfile.native .
```

### Step 5: Start Container

```bash
docker run -d --name local-native-test \
  -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/quarkus_crud \
  local/quarkus-crud-native:latest
```

### Step 6: Wait for Application to Start

```bash
# Wait up to 5 minutes (native starts much faster, usually under 10 seconds)
for i in $(seq 1 60); do
  if curl -sSf http://localhost:8080/healthz >/dev/null 2>&1; then
    echo "Application is ready!"
    break
  fi
  sleep 5
done
```

### Step 7: Run Bash E2E Tests

```bash
BASE_URL=http://localhost:8080 ./scripts/e2e.sh
```

Expected output:
```
[INFO] Starting E2E tests against http://localhost:8080
[TEST] Test 1: Health check endpoint
[INFO] ✓ Test passed: Health check returns 200
...
[INFO] All E2E tests PASSED ✓
```

### Step 8: Run TypeScript E2E Tests

```bash
cd e2e

# Generate TypeScript client from OpenAPI spec
./generate-client.sh

# Install dependencies
npm ci

# Run TypeScript E2E tests
BASE_URL=http://localhost:8080 npm test
```

Expected output:
```
[INFO] Starting TypeScript E2E tests against http://localhost:8080
[TEST] Test 1: Health check endpoint
[INFO] ✓ Test passed: Health check returns 200
...
[INFO] All TypeScript E2E tests PASSED ✓
```

### Step 9: Cleanup

```bash
# Stop and remove containers
docker stop local-native-test || true
docker rm local-native-test || true
docker stop quarkus-crud-postgres || true
docker rm quarkus-crud-postgres || true

# Remove temporary Dockerfile
rm -f Dockerfile.native
```

## Production Testing

To test against the production deployment at `https://quarkus-crud.ouchiserver.aokiapp.com`:

### Bash E2E Tests

```bash
BASE_URL=https://quarkus-crud.ouchiserver.aokiapp.com ./scripts/e2e.sh
```

### TypeScript E2E Tests

```bash
cd e2e

# Generate client (if not already done)
./generate-client.sh

# Install dependencies (if not already done)
npm ci

# Run tests against production
BASE_URL=https://quarkus-crud.ouchiserver.aokiapp.com npm test
```

**Note**: Production testing only reads and creates/deletes test data. It does not modify existing production data.

## Quick Local Test (Using JVM Mode)

For faster iteration during development, you can test in JVM mode:

```bash
# Terminal 1: Start PostgreSQL
docker run --name quarkus-crud-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_crud \
  -p 5432:5432 \
  -d postgres:15-alpine

# Terminal 2: Run application in dev mode
./gradlew quarkusDev

# Terminal 3: Run E2E tests
BASE_URL=http://localhost:8080 ./scripts/e2e.sh

# Optional: Run TypeScript tests
cd e2e && ./generate-client.sh && npm ci && BASE_URL=http://localhost:8080 npm test
```

## Test Coverage

### Bash E2E Tests (`scripts/e2e.sh`)

Tests the following scenarios:

1. Health check endpoint
2. Guest user creation and JWT token extraction
3. Get current user (authenticated)
4. Get current user without token (401 error)
5. List all rooms (public endpoint)
6. Create room (authenticated)
7. Create room without auth (401 error)
8. Get room by ID
9. Update room (owner only)
10. Get my rooms (authenticated)
11. Get non-existent room (404 error)
12. Update room without auth (401 error)
13. Update another user's room (403 error)
14. Delete room (owner only)
15. Verify room deletion (404 after delete)
16. Delete room without auth (401 error)

### TypeScript E2E Tests (`e2e/src/e2e-runner.ts`)

Tests the following scenarios (using generated OpenAPI client):

1. Health check endpoint
2. Guest user creation and JWT token extraction
3. Get current user (authenticated)
4. Get current user without token (401 error)
5. List all rooms (public endpoint)
6. Create room (authenticated)
7. Create room without auth (401 error)
8. Get room by ID
9. Update room (owner only)
10. Get my rooms (authenticated)
11. Get non-existent room (404 error)
12. Update room without auth (401 error)
13. Update another user's room (403 error)
14. Delete room (owner only)
15. Verify room deletion (404 after delete)
16. Delete room without auth (401 error)
17. Create room with empty name (400 validation error)

## Success Criteria

### Expected Results

- **Bash E2E**: All 16 tests should pass with exit code 0
- **TypeScript E2E**: All 17 tests should pass with exit code 0
- **Health check**: Response should contain `"status":"UP"`
- **Authentication**: JWT tokens should be extracted from Authorization headers
- **CRUD operations**: Create, read, update, delete operations should work correctly
- **Authorization**: Owner-only operations should return 403 for non-owners
- **Error handling**: Invalid requests should return appropriate HTTP status codes (400, 401, 403, 404)

### Failure Investigation

If tests fail, check:

1. **Application logs**: `docker logs local-native-test` or check console output in dev mode
2. **PostgreSQL connectivity**: Ensure PostgreSQL is running and accessible
3. **Network**: Ensure the application is listening on port 8080
4. **Environment variables**: Verify `BASE_URL` is set correctly
5. **Test output**: Review the specific test that failed and its error message

## CI/CD Integration

The E2E tests are integrated into the GitHub Actions workflow (`.github/workflows/publish-jib.yml`):

- **JVM variants**: Only health checks are performed (fast verification)
- **Native variants**: Full E2E tests are executed (bash + TypeScript)

The workflow ensures that native images are thoroughly tested before deployment.

## Troubleshooting

### Native Build Fails

```bash
# Check GraalVM installation
java -version

# Try container-based build instead
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true --no-daemon
```

### PostgreSQL Connection Fails

```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check PostgreSQL logs
docker logs quarkus-crud-postgres

# Verify connection manually
docker exec -it quarkus-crud-postgres psql -U postgres -d quarkus_crud -c '\dt'
```

### Application Won't Start

```bash
# Check container logs
docker logs local-native-test

# Verify port is not in use
lsof -i :8080

# Check host.docker.internal resolution
docker exec local-native-test getent hosts host.docker.internal
```

### E2E Tests Timeout

```bash
# Increase wait time in health check loop
# Edit the 'for i in $(seq 1 60)' line to increase iterations

# Check if application is actually ready
curl -v http://localhost:8080/healthz
```

## Additional Resources

- OpenAPI Specification: `src/main/resources/META-INF/openapi.yaml`
- Generated Client: `e2e/generated-client/` (generated, not committed)
- Workflow Definition: `.github/workflows/publish-jib.yml`
