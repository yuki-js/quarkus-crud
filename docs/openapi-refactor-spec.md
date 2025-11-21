# OpenAPI refactor and build pipeline specification

## 1. Purpose and scope

This document describes how to refactor the OpenAPI specification and Gradle build so that:
- The OpenAPI spec is maintained as modular source files under an `openapi/` directory.
- A compile step bundles these sources into a single `src/main/resources/META-INF/openapi.yaml` file for SmallRye OpenAPI and code generation.
- The existing OpenAPI Generator integration continues to work, now using the compiled spec.

The target audience is another implementation agent (AI or developer). Follow these instructions step by step to avoid ambiguity and ensure reproducible results.

## 2. Context

- Project type: Quarkus 3.x, Java 21, Gradle build.
- Existing OpenAPI spec: `src/main/resources/META-INF/openapi.yaml` (single file, rooms-based API).
- Data model: quiz events, users, profiles, friendships, attendees as described in `docs/data-model.md`.
- Build: OpenAPI Generator is already configured in `build.gradle` via the `generateOpenApiModels` task, which currently reads `src/main/resources/META-INF/openapi.yaml`.
- SmallRye OpenAPI: at runtime, Quarkus serves `/q/openapi` and `/q/swagger-ui` from `META-INF/openapi.yaml`.

This spec replaces the legacy "rooms" API with a new API aligned to the current data model and introduces an OpenAPI compilation pipeline.

## 3. High-level deliverables

The implementation must produce all of the following:

1) New OpenAPI 3.0 specification reflecting the new data model and API design (authentication, users, profiles, friendships, events, attendees, SSE, health).
2) Modular OpenAPI source tree under `openapi/` with a clear file layout (root file, paths, schemas).
3) Java-based OpenAPI compiler (`OpenApiCompiler`) that bundles the modular spec into a single YAML file.
4) Gradle configuration updates:
   - New `openApiCompiler` configuration with dependencies.
   - New `compileOpenApi` task.
   - Wiring so `compileOpenApi` runs before `generateOpenApiModels`, and both run before Java compilation and main build tasks.
5) Documentation updates are not required beyond this spec, but the implementation must keep existing docs (such as `docs/api.md`) compiling and tests passing.

## 4. API design summary

The new OpenAPI spec must describe the following API surface. Details such as exact error messages or minor field names can be refined, but the structure and semantics should be preserved.

### 4.1 Common conventions

- JSON property naming: camelCase (e.g. `createdAt`, `accountLifecycle`, `currentProfileRevision`).
- JSONB columns in the database (users.meta, user_profiles.profile_data, user_profiles.revision_meta, events.meta, event_attendees.meta) are exposed as:
  - `type: object`
  - `additionalProperties: true`
  so they are free-form JSON objects.
- Authentication:
  - Single security scheme: `bearerAuth` (HTTP bearer, JWT).
  - JWT is issued by `POST /api/auth/guest`.

### 4.2 Authentication and users

Paths:
- `POST /api/auth/guest`
  - Security: none.
  - Purpose: create an anonymous guest user and return a JWT in the `Authorization` header (`Bearer <token>`).
  - Response: `200` with a `User` representation containing at least `id` and `createdAt`.

- `GET /api/me`
  - Security: `bearerAuth` required.
  - Purpose: return the current authenticated user.
  - Response: `200` with `User`.

- `GET /api/users/{userId}`
  - Security: `bearerAuth`.
  - Purpose: return a public view of the user identified by `userId`.
  - Response: `200` with `UserPublic` (similar to `User` but without sensitive fields).
  - `404` if user not found.

### 4.3 Profiles (user_profiles)

Concept:
- The system keeps immutable profile revisions, but the API exposes only the "current profile" per user.

Paths:
- `GET /api/users/{userId}/profile`
  - Security: `bearerAuth`.
  - Purpose: get the current profile card for the specified user.
  - Response: `200` with `UserProfile` (contains `userId`, `profileData`, `createdAt`, etc.).
  - `404` if user or profile does not exist.

- `GET /api/me/profile`
  - Security: `bearerAuth`.
  - Purpose: get the current profile of the authenticated user.
  - Response: `200` with `UserProfile`.

- `PUT /api/me/profile`
  - Security: `bearerAuth`.
  - Purpose: update the current user's profile; internally this creates a new `user_profiles` row and updates `users.current_profile_revision`.
  - Request body: `UserProfileUpdateRequest` containing `profileData` (free-form object).
  - Response: `200` with the new `UserProfile`.
  - `400` on invalid input.

### 4.4 Friendships (profile card exchanges)

Data model semantics:
- `friendships.senderId` = user who "sends" their profile card.
- `friendships.recipientId` = user who "receives" that card.

For this API:
- `POST /api/users/{userId}/friendship` is interpreted as:
  - "I (the authenticated user) receive the card of `userId`."
  - Therefore: `senderId = userId`, `recipientId = me`.

Paths:
- `POST /api/users/{userId}/friendship`
  - Security: `bearerAuth`.
  - Purpose: register that the current user has received the profile card of `userId` (e.g. by scanning a QR code).
  - Request body: empty or optional `meta` object.
  - Response: `201` with `Friendship` (id, senderUserId, recipientUserId, createdAt, etc.).

- `GET /api/me/friendships/received`
  - Security: `bearerAuth`.
  - Purpose: list friendships where the current user is the recipient (`recipientId = me`).
  - Response: `200` with `Friendship[]`.
  - Optionally, each entry may include a nested sender profile summary.

### 4.5 Events, invitation codes, and attendees

General:
- `events`:
  - `id`, `initiatorId`, `status` (`created`, `active`, `ended`, `expired`, `deleted`), `meta`, `expiresAt`, timestamps.
- `event_attendees`:
  - `id`, `eventId`, `attendeeUserId`, `meta`, timestamps.
- `event_invitation_codes`:
  - Represented only indirectly through API; no direct CRUD endpoints in V1.

Paths:
- `POST /api/events`
  - Security: `bearerAuth`.
  - Purpose: create a new event with current user as initiator.
  - Request body: `EventCreateRequest` (fields like `meta`, `expiresAt`).
  - Response: `201` with `Event`.

- `GET /api/events/{eventId}`
  - Security: `bearerAuth`.
  - Purpose: fetch event details.
  - Response: `200` with `Event`.
  - `404` if not found.

- `GET /api/users/{userId}/events`
  - Security: `bearerAuth`.
  - Purpose: list events initiated by the specified user.
  - Response: `200` with `Event[]`.

- `POST /api/events/join-by-code`
  - Security: `bearerAuth`.
  - Purpose: join an event by invitation code.
  - Request body: `EventJoinByCodeRequest` with `invitationCode: string`.
  - Server behavior:
    - Look up `event_invitation_codes` by `invitationCode`.
    - Create an `event_attendees` row `(eventId, attendeeUserId = me)`.
  - Response: `201` with `EventAttendee` for the current user in that event.
  - Errors: `400` (invalid code), `404` (no matching active event), `409` (already joined) as appropriate.

- `GET /api/events/{eventId}/attendees`
  - Security: `bearerAuth`.
  - Purpose: list all attendees of the event.
  - Response: `200` with `EventAttendee[]`.

### 4.6 SSE: event-level live updates

SSE is used only for participant-related events inside a given event.

Path:
- `GET /api/events/{eventId}/live`
  - Security: `bearerAuth` (implementation should typically restrict access to attendees).
  - Content type: `text/event-stream`.
  - Purpose: stream changes in event attendees.
  - Event payload schema (logical model, to be defined in OpenAPI as a string or dedicated schema):
    - `eventType`: enum `ATTENDEE_JOINED`, `ATTENDEE_LEFT`.
    - `eventId`: int64.
    - `attendeeUserId`: int64.
    - `timestamp`: string (date-time).

### 4.7 Health

Path:
- `GET /healthz`
  - Security: none.
  - Purpose: legacy/simple health check used by external systems.
  - Responses:
    - `200` with body like `{ "status": "UP", "service": "quarkus-crud" }`.
    - `503` with `{ "status": "DOWN", "service": "quarkus-crud" }`.

### 4.8 Core schemas

Define the following schemas in `components/schemas` (names are indicative; implementer may adjust but should keep meaning stable):

- `User`
- `UserPublic`
- `UserProfile`
- `UserProfileUpdateRequest`
- `Friendship`
- `Event`
- `EventCreateRequest`
- `EventAttendee`
- `EventJoinByCodeRequest`
- `EventLiveEvent` (SSE payload, if modeled as a schema)
- `ErrorResponse` (with at least `error: string`)
- `LocalDateTime` (string, date-time)

## 5. OpenAPI source layout under openapi/

Implement the following directory structure at the project root:

- `openapi/openapi.yaml` – root spec file.
- `openapi/paths/auth.yaml`
- `openapi/paths/users.yaml`
- `openapi/paths/profiles.yaml`
- `openapi/paths/friendships.yaml`
- `openapi/paths/events.yaml`
- `openapi/paths/health.yaml`
- `openapi/components/schemas/user.yaml`
- `openapi/components/schemas/profile.yaml`
- `openapi/components/schemas/friendship.yaml`
- `openapi/components/schemas/event.yaml`
- `openapi/components/schemas/eventAttendee.yaml`
- `openapi/components/schemas/common.yaml`

Guidelines:
- Keep each path file focused on related endpoints (e.g. auth endpoints in `auth.yaml`, events endpoints in `events.yaml`).
- In `openapi/openapi.yaml`, use `$ref` to delegate path definitions and schema definitions to the corresponding files.
- Ensure relative `$ref` paths are correct and compatible with Swagger Parser (e.g. `./paths/auth.yaml#/paths/~1api~1auth~1guest`).

The exact YAML content is left to the implementer, but it must faithfully represent the API design in section 4.

## 6. OpenApiCompiler Java utility

Create a Java class that compiles the modular spec into a single YAML file:

- Package: `app.aoki.quarkuscrud.openapi`.
- Class name: `OpenApiCompiler`.
- Location: `src/main/java/app/aoki/quarkuscrud/openapi/OpenApiCompiler.java`.
- Entry point: `public static void main(String[] args)`.

Required behavior:
- Arguments:
  - `args[0]`: absolute or relative path to the root OpenAPI spec (e.g. `openapi/openapi.yaml`).
  - `args[1]`: absolute or relative path to the output file (e.g. `src/main/resources/META-INF/openapi.yaml`).
- Steps:
  1) Validate that exactly 2 arguments are provided; otherwise print usage to stderr and exit with non-zero status.
  2) Use Swagger Parser v3 to load the spec and resolve external `$ref` references.
  3) Optionally run `ResolverFully` to resolve in-model references.
  4) Serialize the resulting `OpenAPI` object back to YAML.
  5) Ensure the parent directory of the output file exists; create it if necessary.
  6) Write the YAML to the output path, overwriting any existing file.
  7) On parse errors or validation issues, print all messages to stderr and exit with non-zero status.

Implementation hints:
- Use:
  - `io.swagger.v3.parser.OpenAPIV3Parser`
  - `io.swagger.v3.parser.core.models.SwaggerParseResult`
  - `io.swagger.v3.parser.util.ResolverFully`
  - `io.swagger.v3.core.util.Yaml`
- Do not rely on Quarkus runtime; this must be a plain Java main class callable from Gradle.

## 7. Gradle configuration changes

All changes are in `build.gradle`. The goals are:
- Add a configuration and dependencies for the OpenAPI compiler.
- Define a `compileOpenApi` task that uses `OpenApiCompiler`.
- Ensure `generateOpenApiModels` depends on `compileOpenApi`.
- Keep existing `generateOpenApiModels` behavior, but use the compiled spec.

### 7.1 Add configuration and dependencies

Extend the existing `configurations` block:

- Ensure it contains:
  - `openApiGenerator` (already present).
  - `openApiCompiler` (new).

In the dependencies section associated with OpenAPI tooling:
- Keep the existing:
  - `openApiGenerator 'org.openapitools:openapi-generator-cli:7.10.0'`.
- Add:
  - `openApiCompiler 'io.swagger.parser.v3:swagger-parser-v3:<suitable-version>'`.
  - `openApiCompiler 'io.swagger.core.v3:swagger-core:<matching-version>'`.
- Choose versions compatible with the project; if uncertain, use current stable versions and ensure they resolve.

### 7.2 compileOpenApi task

Define a new `JavaExec` task named `compileOpenApi`:

- Group: `openapi`.
- Description: `"Compile modular OpenAPI sources in openapi/ into META-INF/openapi.yaml"`.
- `classpath = configurations.openApiCompiler`.
- `mainClass = 'app.aoki.quarkuscrud.openapi.OpenApiCompiler'`.
- Inputs:
  - `inputSpec = file('openapi/openapi.yaml')`.
- Outputs:
  - `outputSpec = file('src/main/resources/META-INF/openapi.yaml')`.
- Configure task inputs/outputs for incremental build:
  - `inputs.file(inputSpec)`.
  - `outputs.file(outputSpec)`.
- Args:
  - `args inputSpec.absolutePath, outputSpec.absolutePath`.

This task should not start Quarkus or require any external services.

### 7.3 Wire generateOpenApiModels to compileOpenApi

The existing `generateOpenApiModels` task already:
- Uses `src/main/resources/META-INF/openapi.yaml` as `inputSpec`.
- Writes generated sources into `build/generated-src/openapi/src/gen/java`.
- Is wired to `compileJava` and other tasks via `dependsOn`.

Update `build.gradle` so that:
- `generateOpenApiModels.dependsOn compileOpenApi`.

Use Gradle's `tasks.named` or direct reference, consistent with existing style. After this change, the order will be:
- `compileOpenApi` → `generateOpenApiModels` → `compileJava`.
- Any task that depends on `generateOpenApiModels` automatically depends on `compileOpenApi` as well.

There is no need to change the `inputSpec` path in `generateOpenApiModels`; it should continue to point to `src/main/resources/META-INF/openapi.yaml`, which is now generated.

### 7.4 Clean task adjustments

The `clean` task already deletes `build/generated-src`.
Ensure that cleaning generated OpenAPI models is preserved.

The compiled `src/main/resources/META-INF/openapi.yaml` is part of the source tree; depending on the team's preference it may or may not be removed by `clean`. If you decide to treat it as a generated artifact, you may extend `clean` to delete it as well. If you do so, document it in this spec and keep the behavior consistent.

For now, it is acceptable to leave `META-INF/openapi.yaml` in place (it will be overwritten by the next `compileOpenApi` run).

## 8. Verification steps

After implementing the above changes, verify them as follows:

1) OpenAPI compilation:
  - Run:
    - `./gradlew compileOpenApi`.
  - Confirm:
    - The task completes successfully.
    - `src/main/resources/META-INF/openapi.yaml` exists and is non-empty.
    - The file content corresponds to the modular sources and passes a basic sanity check (e.g. open in an OpenAPI viewer or lint with Spectral if available).

2) Code generation:
  - Run:
    - `./gradlew generateOpenApiModels`.
  - Confirm:
    - The task runs `compileOpenApi` first.
    - Generated sources appear under `build/generated-src/openapi/src/gen/java`.
    - No validation errors are reported from the OpenAPI Generator.

3) Build and tests:
  - Run:
    - `./gradlew build`.
  - Confirm:
    - The build succeeds.
    - Tests pass.

4) Runtime verification:
  - Run:
    - `./gradlew quarkusDev`.
  - Confirm:
    - Quarkus starts successfully.
    - `http://localhost:8080/q/openapi` returns the new spec (no references to legacy `/api/rooms`).
    - `http://localhost:8080/q/swagger-ui` shows the new endpoints defined in section 4 (authentication, users, profiles, friendships, events, SSE, health).

## 9. Non-goals and constraints

- Do not modify business logic, database migrations, or MyBatis mappers as part of this task. Only the OpenAPI spec, the OpenAPI compilation pipeline, and Gradle integration are in scope.
- Do not remove or break existing tests. If tests assume old endpoints (e.g. `/api/rooms`), coordinate separately before changing them; this spec describes only the API and build side.
- Keep the implementation deterministic and idempotent: running `compileOpenApi` or `generateOpenApiModels` multiple times should always produce the same outputs for the同じ入力に対して同じ出力となるようにすること。

This completes the implementation-ready specification for another AI or developer to follow when refactoring the OpenAPI spec and Gradle build.