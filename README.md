# quarkus-template

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## API Documentation

The API is documented using OpenAPI 3.0.3:

- **OpenAPI Specification**: Available at <http://localhost:8080/openapi>
- **Swagger UI**: Interactive API documentation at <http://localhost:8080/swagger-ui>
- **Source**: The authoritative spec is in `src/main/resources/META-INF/openapi.yaml`

## OpenAPI-First Development Workflow

This project uses an OpenAPI-first approach where the OpenAPI specification is the single source of truth for the API contract.

### Key Components

1. **OpenAPI Specification** (`src/main/resources/META-INF/openapi.yaml`)
   - Authoritative API definition
   - Documents all endpoints, request/response schemas, and validation rules
   - Must be updated BEFORE implementing new endpoints

2. **Generated DTOs** (in `build/generated-src/openapi/`)
   - Strongly-typed Java models generated from OpenAPI spec
   - Include Bean Validation annotations (@NotNull, @Size, etc.)
   - Not committed to repository (generated on build)

3. **MapStruct Mappers** (`src/main/java/app/aoki/mapper/dto/`)
   - Convert between generated DTOs and entity classes
   - Handle type conversions (e.g., LocalDateTime ↔ OffsetDateTime)
   - Use CDI for dependency injection

4. **Exception Mappers** (`src/main/java/app/aoki/exception/`)
   - Ensure consistent error response format
   - Handle validation errors with descriptive messages

5. **Contract Tests** (`src/test/java/app/aoki/OpenApiContractTest.java`)
   - Validate actual API responses against OpenAPI spec
   - Ensure implementation matches documentation

### Workflow for Adding New Endpoints

1. **Update OpenAPI Specification**
   ```bash
   # Edit src/main/resources/META-INF/openapi.yaml
   # Add new endpoint definition with schemas
   ```

2. **Generate DTOs**
   ```bash
   ./gradlew generateOpenApiModels
   ```

3. **Create/Update MapStruct Mappers**
   ```bash
   # Add mapper methods in src/main/java/app/aoki/mapper/dto/
   # MapStruct will generate implementations at compile time
   ```

4. **Implement Resource Method**
   ```java
   @Inject MyDtoMapper mapper;
   
   @GET
   @Path("/my-endpoint")
   public Response getMyData() {
       // Use generated DTO and mapper
       var entity = service.findData();
       var dto = mapper.toDto(entity);
       return Response.ok(dto).build();
   }
   ```

5. **Add Contract Test**
   ```java
   @Test
   public void testMyEndpointContract() {
       given()
           .filter(validationFilter)
           .when()
           .get("/api/my-endpoint")
           .then()
           .statusCode(200);
   }
   ```

6. **Validate**
   ```bash
   ./gradlew build  # Runs all tests including contract tests
   ```

### Useful Commands

**Generate OpenAPI models:**
```bash
./gradlew generateOpenApiModels
```

**Validate OpenAPI spec:**
```bash
# Using OpenAPI Generator CLI
java -jar openapi-generator-cli.jar validate -i src/main/resources/META-INF/openapi.yaml

# Using Spectral (requires Node.js)
npm install -g @stoplight/spectral-cli
spectral lint src/main/resources/META-INF/openapi.yaml
```

**Run contract tests only:**
```bash
./gradlew test --tests OpenApiContractTest
```

**View generated models:**
```bash
ls -la build/generated-src/openapi/src/main/java/app/aoki/generated/model/
```

### CI/CD Integration

The `.github/workflows/openapi-ci.yml` workflow automatically:
- Validates the OpenAPI specification
- Generates models and verifies compilation
- Runs contract tests
- Fails the build if spec and implementation diverge

### Migration Strategy

Currently, the project is gradually migrating to use generated DTOs:
- ✅ **GET /api/rooms** - Uses generated `RoomResponse` DTO (example implementation)
- ⏳ Other endpoints still use nested record types

To migrate an endpoint:
1. Ensure OpenAPI spec defines the request/response schemas
2. Run `./gradlew generateOpenApiModels`
3. Update resource method to use generated DTO and mapper
4. Verify tests pass (especially contract tests)

### Benefits

- **Single Source of Truth**: OpenAPI spec documents the API contract
- **Type Safety**: Generated DTOs prevent runtime errors
- **Validation**: Bean Validation annotations enforce constraints
- **Contract Testing**: Automatically verify implementation matches spec
- **Better Documentation**: Swagger UI always reflects actual API

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/quarkus-template-0.0.1-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.

## Code Formatting and Linting

This project uses automated tools to maintain code quality, similar to ESLint/Prettier for JavaScript:

- **[Spotless](https://github.com/diffplug/spotless)** with **[Google Java Format](https://github.com/google/google-java-format)** for code formatting (like Prettier)
- **[Checkstyle](https://checkstyle.org/)** for code quality checks and linting (like ESLint)

### Available Commands

**Check code formatting:**
```shell script
./gradlew spotlessCheck
```
Verifies that all Java files follow the formatting rules without making any changes.

**Apply code formatting:**
```shell script
./gradlew spotlessApply
```
Automatically formats all Java files according to Google Java Format style.

**Check code quality (linting):**
```shell script
./gradlew checkstyleMain checkstyleTest
```
Analyzes code for potential issues, violations of coding standards, and best practices. Generates an HTML report at `build/reports/checkstyle/main.html`.

**Run all checks:**
```shell script
./gradlew spotlessCheck checkstyleMain checkstyleTest
```
Runs both formatting and linting checks (used in CI).

**Integration with your workflow:**
- Run `./gradlew spotlessApply` before committing to ensure consistent formatting
- Run `./gradlew checkstyleMain` to check for code quality issues
- View detailed reports: `build/reports/checkstyle/main.html`
- Both checks run automatically in CI/CD pipeline
- **Formatter** (Spotless) handles: indentation, import ordering, whitespace
- **Linter** (Checkstyle) checks: star imports, redundant modifiers, naming conventions, common issues, and best practices

## Code Style Guidelines

### API Input/Output Models

All API request and response models are defined as nested types within their respective Resource classes, using Java records where possible. This approach:

- **Improves locality**: Request/response models are co-located with the endpoints that use them
- **Reduces file scatter**: No separate DTO packages to navigate
- **Enhances security**: Sensitive fields are easier to audit when they're close to the API definition
- **Leverages modern Java**: Records provide immutable, concise data carriers

**Naming conventions:**
- Input models: `CreateXxxRequest`, `UpdateXxxRequest`
- Output models: `XxxResponse`, `XxxRepresentation`
- Avoid the term "DTO" in naming

**Example:**
```java
@Path("/api/rooms")
public class RoomResource {
    
    public static record CreateRoomRequest(String name, String description) {}
    
    public static record RoomResponse(Long id, String name, Long userId, LocalDateTime createdAt) {
        public static RoomResponse from(Room entity) {
            return new RoomResponse(entity.getId(), entity.getName(), 
                                   entity.getUserId(), entity.getCreatedAt());
        }
    }
    
    @POST
    public Response createRoom(CreateRoomRequest request) {
        // Implementation
    }
}
```

**Security note:** Never include sensitive fields (like authentication tokens) in response models. Use HttpOnly cookies or server-side storage instead.

### Troubleshooting

**Problem: Generated models not found during compilation**
```bash
# Solution: Generate models before compiling
./gradlew generateOpenApiModels
./gradlew build
```

**Problem: MapStruct mapper implementation not generated**
```bash
# Solution: Clean and rebuild
./gradlew clean
./gradlew generateOpenApiModels
./gradlew build
```

**Problem: Contract tests failing**
- Check that OpenAPI spec matches actual implementation
- Verify response JSON structure matches schema definitions
- Ensure status codes in spec match those returned by endpoints

**Problem: Validation not working**
- Ensure generated DTOs have `@NotNull`, `@Size`, etc. annotations
- Verify `quarkus-hibernate-validator` is in dependencies
- Check that exception mappers are registered

### Best Practices

1. **Always update OpenAPI spec first** before implementing new endpoints
2. **Run contract tests** to verify spec and implementation match
3. **Use descriptive operation IDs** in OpenAPI spec for better generated code
4. **Document validation rules** in OpenAPI spec (minLength, maxLength, pattern, etc.)
5. **Keep generated code separate** - never edit files in `build/generated-src/`
6. **Use MapStruct mappers** for all DTO conversions to maintain consistency
7. **Preserve error format** - use exception mappers to maintain `{"error": "message"}` shape

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- MyBatis SQL Mapper ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-mybatis/dev/index.html)): MyBatis SQL mapper framework for Java
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- OpenAPI Generator ([website](https://openapi-generator.tech/)): Generate client SDKs, server stubs, and documentation from OpenAPI specs
- MapStruct ([guide](https://mapstruct.org/)): Java bean mappings, the easy way!
- Swagger Request Validator ([github](https://github.com/atlassian-labs/swagger-request-validator)): Validate API requests and responses against OpenAPI specs
