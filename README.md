# quarkus-template

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

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

## Code Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with [Google Java Format](https://github.com/google/google-java-format) to maintain consistent code style across the codebase, similar to how ESLint/Prettier work for JavaScript projects.

### Available Commands

**Check code formatting:**
```shell script
./gradlew spotlessCheck
```
This command verifies that all Java files follow the formatting rules without making any changes. Use this in CI/CD pipelines or before committing.

**Apply code formatting:**
```shell script
./gradlew spotlessApply
```
This command automatically formats all Java files according to the configured style. Run this before committing your changes.

**Integration with your workflow:**
- Run `./gradlew spotlessApply` before committing to ensure consistent formatting
- The formatter handles indentation, import ordering, whitespace, and more
- All Java files in `src/` directory are automatically formatted

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

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- MyBatis SQL Mapper ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-mybatis/dev/index.html)): MyBatis SQL mapper framework for Java
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
