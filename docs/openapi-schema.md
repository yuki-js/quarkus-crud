# Schema-First OpenAPI Workflow

The template treats the OpenAPI contract as the authoritative description of the service. Everything else—resource interfaces, validation annotations, TypeScript clients—flows from that contract. This document outlines the layout under `openapi/`, the Gradle tasks that stitch it together, and how generated code is used inside the Quarkus application.

## Source layout

The `openapi/` directory contains:

- `openapi.yaml`: the root file that references modular pieces.
- `paths/`: per-endpoint YAML fragments.
- `components/`: schemas, parameters, responses, and shared definitions.

Write or edit schemas in these modules rather than touching the generated Java files. Keeping the source modular avoids thousand-line monolith specs and keeps merge conflicts sane.

## Compilation pipeline

1. **compileOpenApi** — runs `buildutil.OpenApiCompiler` (from `buildSrc`) to resolve `$ref`s, bundle the modular spec, and emit a single `build/openapi-compiled/openapi.yaml`. This step also validates the document and fails loudly on syntax issues.
2. **generateOpenApiModels** — feeds the compiled spec into OpenAPI Generator (`jaxrs-spec` target) to create:
   - Interfaces under `app.aoki.quarkuscrud.generated.api` with annotated JAX-RS endpoints.
   - Model classes under `app.aoki.quarkuscrud.generated.model` with Bean Validation metadata.
   The generated sources land in `build/generated-src/openapi/src/gen/java` and are added to the `main` source set automatically.
3. **Application code** — concrete resources live in `src/main/java/app/aoki/quarkuscrud/resource` and implement the generated interfaces. Because interfaces return `Response` and carry validation annotations, the compilation catches mismatches early.
4. **Contract enforcement** — `OpenApiContractTest` uses `swagger-request-validator` + RestAssured to compare runtime responses with the compiled spec. Break the contract and tests warn you before reviewers do.

All top-level Gradle tasks (`build`, `test`, `quarkusDev`, `jib`, etc.) depend on `generateOpenApiModels`, so you rarely need to invoke the tasks manually. If you are editing only the spec and want fast feedback, `./gradlew compileOpenApi` suffices.

## Working with the spec

- **Adding endpoints**: create a new YAML under `openapi/paths`, reference shared schemas from `components`, wire the path into `openapi.yaml`, and run `./gradlew compileOpenApi`. Then implement the generated interface stub in the `resource` package.
- **Evolving models**: update schemas, regenerate (`./gradlew generateOpenApiModels`), and fix compiler errors in mappers/service layers that relied on the old shape. Because Bean Validation annotations live on the generated classes, they stay in sync with the contract.
- **Sharing artifacts**: The compiled `openapi.yaml` is copied into `build/resources/main/META-INF` so SmallRye OpenAPI serves the exact same document at runtime (`/openapi`).

Consider the schema the source of truth, not the code. This keeps backend, client, and documentation aligned without manual bookkeeping.
