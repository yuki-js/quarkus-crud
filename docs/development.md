# Development Workflow

The template is built so that you can pull the repo, start Dev Services, and iterate on the OpenAPI contract and code without touching manual infrastructure. This document explains how the local loop works and which Gradle tasks back the automation.

## Prerequisites

Install Java 21 (Temurin or any distribution that works with Gradle toolchains), Docker if you want Quarkus Dev Services to spin up PostgreSQL automatically, and Node.js if you plan to build the generated JavaScript client locally. The Gradle wrapper is included, so you do not need a global Gradle install.

## Day-to-day loop

1. Start dev mode with:

   ```bash
   ./gradlew quarkusDev
   ```

   Quarkus boots the application, launches Dev Services PostgreSQL (reusing the container between runs), applies Flyway migrations, compiles the modular OpenAPI spec (`compileOpenApi`), regenerates Java interfaces and models (`generateOpenApiModels`), and triggers the TypeScript fetch client build. Dev UI is available at `http://localhost:8080/q/dev-ui`, Swagger UI at `/q/swagger-ui`, Prometheus metrics at `/q/metrics`, and health checks at `/healthz` (prod style) or `/q/health` (dev profile).

2. Edit code or OpenAPI fragments. Live reload covers both Java sources and the OpenAPI tree. Recompilation happens automatically because `compileJava` depends on `generateOpenApiModels`, which itself depends on the compiler task.

3. Watch continuous testing from the dev console. The template enables `quarkus.test.continuous-testing=enabled`, so unit tests rerun the moment relevant files change. Use `r` in the dev console to re-run, `o` to toggle output, or `p` to pause.

4. When you need a clean rebuild (for example, after upgrading dependencies), run `./gradlew clean build`. This ensures generated sources under `build/generated-src/openapi` are recreated and the JavaScript client tarball is rebuilt under `build/distributions/`.

## Running tests explicitly

`./gradlew test` executes the full suite: Quarkus integration tests backed by Dev Services, RestAssured contract checks, and Swagger request-validation. You can scope tests (e.g., `./gradlew :test --tests '*UserServiceTest'`) when you only care about a subset. Remember that contract tests rely on the compiled OpenAPI spec in `build/openapi-compiled/openapi.yaml`, so if you edit OpenAPI modules outside the Gradle lifecycle, run `./gradlew compileOpenApi` first to avoid stale contracts.

## Tooling niceties

- **Spotless** keeps Java sources formatted with Google Java Format. Run `./gradlew spotlessApply` before committing if your editor does not handle it.
- **Checkstyle** is available via `./gradlew checkstyleMain`. Violations show up as warnings because the goal is signal, not friction.
- **Dev UI extensions** (quarkus-info) surface build metadata, git info, and Dev Services status. Use it to spot configuration drift early in the cycle.

Stick to this loop and you get deterministic builds: every change to OpenAPI, schema, or code flows through Gradle once, and the generated artifacts stay in sync with the running application.
