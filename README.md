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

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- MyBatis SQL Mapper ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-mybatis/dev/index.html)): MyBatis SQL mapper framework for Java
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC

## Container Images with Jib

This project uses [Jib](https://github.com/GoogleContainerTools/jib) to build container images without requiring a Docker daemon. Images are published to GitHub Container Registry (GHCR).

### Available Image Variants

Four image variants are built:

1. **JVM Normal** (`<sha>-jvm`): Lightweight JVM image based on distroless
2. **JVM Debug** (`<sha>-jvm-debug`): Debug-friendly JVM image based on eclipse-temurin (includes shell and debugging tools)
3. **Native Normal** (`<sha>-native`): Native executable image based on distroless (smallest, fastest startup)
4. **Native Debug** (`<sha>-native-debug`): Debug-friendly native image based on eclipse-temurin

> **Note**: We use `eclipse-temurin:21-jre` as the debug base image instead of `debian:bookworm-slim` due to compatibility with Jib's OCI manifest handling.

### Local Build and Push

#### Prerequisites

To push images locally, you need a GitHub Personal Access Token (PAT) with the following scopes:
- `write:packages`
- `read:packages`

Create a PAT at: https://github.com/settings/tokens/new

#### Building JVM Images

```bash
# Build the application
./gradlew build

# Push JVM normal image (distroless)
./gradlew jib \
  -Djib.from.image=gcr.io/distroless/java21-debian12 \
  -Djib.to.image=ghcr.io/yuki-js/quarkus-crud:<sha>-jvm \
  -Djib.to.auth.username=<YOUR_GITHUB_USERNAME> \
  -Djib.to.auth.password=<YOUR_PAT>

# Push JVM debug image (eclipse-temurin)
./gradlew jib \
  -Djib.from.image=eclipse-temurin:21-jre \
  -Djib.to.image=ghcr.io/yuki-js/quarkus-crud:<sha>-jvm-debug \
  -Djib.to.auth.username=<YOUR_GITHUB_USERNAME> \
  -Djib.to.auth.password=<YOUR_PAT>
```

#### Building Native Images

> **Tip**: Use the `scripts/prepare-native.sh` helper script to automate the preparation of the native executable for Jib.

```bash
# Build native executable (requires GraalVM)
./gradlew build -Dquarkus.package.type=native

# Prepare native executable for Jib (using helper script)
./scripts/prepare-native.sh

# Or manually:
# mkdir -p build/jib-native
# NATIVE_RUNNER=$(find build -name '*-runner' -type f ! -path "*/quarkus-app/*" | head -n 1)
# cp "$NATIVE_RUNNER" build/jib-native/quarkus-run
# chmod +x build/jib-native/quarkus-run

# Push native normal image (distroless)
./gradlew jib -PnativeBuild \
  -Djib.from.image=gcr.io/distroless/java21-debian12 \
  -Djib.to.image=ghcr.io/yuki-js/quarkus-crud:<sha>-native \
  -Djib.to.auth.username=<YOUR_GITHUB_USERNAME> \
  -Djib.to.auth.password=<YOUR_PAT>

# Push native debug image (eclipse-temurin)
./gradlew jib -PnativeBuild \
  -Djib.from.image=eclipse-temurin:21-jre \
  -Djib.to.image=ghcr.io/yuki-js/quarkus-crud:<sha>-native-debug \
  -Djib.to.auth.username=<YOUR_GITHUB_USERNAME> \
  -Djib.to.auth.password=<YOUR_PAT>
```

### Pulling and Running Images

#### Pull Images from GHCR

```bash
# Authenticate with GHCR
echo <YOUR_PAT> | docker login ghcr.io -u <YOUR_GITHUB_USERNAME> --password-stdin

# Pull images
docker pull ghcr.io/yuki-js/quarkus-crud:<sha>-jvm
docker pull ghcr.io/yuki-js/quarkus-crud:<sha>-jvm-debug
docker pull ghcr.io/yuki-js/quarkus-crud:<sha>-native
docker pull ghcr.io/yuki-js/quarkus-crud:<sha>-native-debug
```

#### Run Images

```bash
# Run JVM image
docker run -p 8080:8080 ghcr.io/yuki-js/quarkus-crud:<sha>-jvm

# Run JVM debug image (with shell access)
docker run -it -p 8080:8080 ghcr.io/yuki-js/quarkus-crud:<sha>-jvm-debug

# Run native image (fastest startup)
docker run -p 8080:8080 ghcr.io/yuki-js/quarkus-crud:<sha>-native

# Run native debug image
docker run -it -p 8080:8080 ghcr.io/yuki-js/quarkus-crud:<sha>-native-debug
```

#### Health Check

If the application has a health endpoint:

```bash
curl http://localhost:8080/q/health
```

### CI/CD Automation

Images are automatically built and pushed to GHCR by the GitHub Actions workflow (`.github/workflows/publish-jib.yml`) on:
- Push to `main` branch
- Tags matching `v*` pattern
- Manual workflow dispatch

### Image Size Comparison

Approximate image sizes (uncompressed):
- **Native Normal**: ~50-100 MB (smallest)
- **JVM Normal**: ~200-250 MB
- **Native Debug**: ~200-300 MB
- **JVM Debug**: ~400-500 MB (largest, includes JDK and debugging tools)

> **Note**: Debug images (eclipse-temurin based) are significantly larger and should only be used for debugging purposes.

### Making Images Public on GHCR

By default, images pushed to GHCR are private. To make them public:

1. Go to https://github.com/users/yuki-js/packages/container/quarkus-crud/settings
2. Scroll to "Danger Zone"
3. Click "Change visibility" and select "Public"
4. Confirm the change

Alternatively, use the GitHub CLI:

```bash
gh api \
  --method PATCH \
  -H "Accept: application/vnd.github+json" \
  /user/packages/container/quarkus-crud/settings \
  -f visibility='public'
```
