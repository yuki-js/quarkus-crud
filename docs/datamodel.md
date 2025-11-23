# Data Model & Persistence

Persistence is implemented with PostgreSQL, Flyway, and MyBatis. The Flyway migrations under `src/main/resources/db/migration/` define the canonical schema while the MyBatis mappers translate between generated OpenAPI models and SQL operations. This document explains the tables created by `V1__Initial_schema.sql` and how they relate to the application services.

## Users and authentication

`users` is the root aggregate that tracks lifecycle state, current profile revision, JSONB metadata, and timestamps. The `account_lifecycle` column is indexed so the service can filter on states such as `created`, `active`, or `paused`. `authn_providers` hangs off `users` through `user_id` and stores authentication method (`anonymous`, `oidc`, etc.), identifiers, and external subjects. Partial unique indexes enforce “one anonymous identity per identifier” and “one external subject per method”, giving the service room to mix multiple auth sources without collisions.

## Profiles

`user_profiles` stores immutable profile revisions (JSONB payload plus optional revision metadata). Each profile row references the owning user; the `users.current_profile_revision` foreign key back to `user_profiles` points at the “current” revision so reads are cheap while history remains intact. Services swap revisions by inserting a new profile row and updating the pointer rather than mutating JSON in place.

## Friendships

`friendships` represents unidirectional sharing between two users. Keeping it as a simple table with sender/recipient ids makes it trivial to model “cards” being sent one way while still enforcing uniqueness on the pair. Cascade deletes on both foreign keys mean removing a user cleans up their friendships automatically.

## Events

The `events` table models quiz sessions with an `initiator_id`, `status`, optional `meta` JSONB payload, and `expires_at`. Several indexes exist so the application can query by status or expiration for housekeeping jobs. Invitation codes live in their own table (`event_invitation_codes`) keyed by `event_id`; this keeps code lookups fast and allows partial uniqueness rules at the application level where PostgreSQL predicates fall short.

`event_attendees` records who joined a given event. Each row references the event and the attending user plus per-attendee metadata. A unique index on `(event_id, attendee_user_id)` guarantees that a user only shows up once per event.

## Migration workflow

Flyway is configured to run at startup (`quarkus.flyway.migrate-at-start=true`). Development mode also enables baseline-on-migrate so fresh Dev Services databases accept the first migration without manual bootstrapping. To add a change, create a new SQL file (e.g., `V2__add_scores.sql`) under the migration folder, update the corresponding MyBatis mapper, and let Flyway apply it automatically the next time the app boots.

## MyBatis integration

Mappers live under `src/main/java/app/aoki/quarkuscrud/mapper`. They translate between generated DTOs and SQL statements, keeping complex joins or JSONB updates inside mapper XML/annotations rather than scattering SQL strings throughout services. Because Quarkus manages the MyBatis session factory, you can inject mappers directly into services and rely on CDI scopes for lifecycle management.
