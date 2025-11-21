# Data Model Documentation

## Overview

This document describes the data model for the Quarkus CRUD application. The model has been designed to support quiz events with user authentication, profile management, and social connections.

## Database Tables

### 1. Users

The core user entity that tracks user lifecycle and profile information.

**Table: `users`**

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK, IDENTITY) | Auto-generated primary key |
| account_lifecycle | VARCHAR(50) | Account lifecycle state (created, provisioned, active, paused, deleted) |
| current_profile_revision | BIGINT (FK) | Reference to the current profile revision |
| meta | JSONB | Flexible metadata (e.g., pause reason) |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_users_lifecycle` on `account_lifecycle`
- `idx_users_created_at` on `created_at DESC`

**Lifecycle States:**
- `created`: Account has been created but not fully provisioned
- `provisioned`: Account is ready for login (external entities created)
- `active`: Account is active and has completed initial login
- `paused`: Account is temporarily suspended
- `deleted`: Account is deleted

### 2. Authentication Providers

Stores authentication information for users. A user can have multiple authentication methods.

**Table: `authn_providers`**

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK, IDENTITY) | Auto-generated primary key |
| user_id | BIGINT (FK) | Reference to users table |
| auth_method | VARCHAR(50) | Authentication method (anonymous, oidc) |
| auth_identifier | VARCHAR(255) | Internal authentication identifier (for anonymous) |
| external_subject | VARCHAR(255) | Subject from external provider (for OIDC) |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_authn_providers_user_id` on `user_id`
- `idx_authn_providers_auth_identifier` on `auth_identifier`
- `idx_authn_providers_method_external` on `(auth_method, external_subject)`
- Unique index on `auth_identifier` for anonymous auth
- Unique index on `(auth_method, external_subject)` for external auth

**Foreign Keys:**
- `user_id` → `users(id)` ON DELETE CASCADE

### 3. User Profiles

Immutable profile revisions for users. Each record represents a profile snapshot.

**Table: `user_profiles`**

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK, IDENTITY) | Auto-generated primary key |
| user_id | BIGINT (FK) | Reference to users table |
| profile_data | JSONB | Profile card data (immutable) |
| revision_meta | JSONB | Metadata about this revision (mutable) |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_user_profiles_user_id` on `user_id`
- `idx_user_profiles_created_at` on `created_at DESC`

**Foreign Keys:**
- `user_id` → `users(id)` ON DELETE CASCADE

**Notes:**
- Profile data is immutable; only revision_meta can be updated
- Latest profile for a user is cached in `users.current_profile_revision`

### 4. Friendships

Unidirectional many-to-many relationship for profile card exchanges.

**Table: `friendships`**

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK, IDENTITY) | Auto-generated primary key |
| sender_id | BIGINT (FK) | User who sent their profile card |
| recipient_id | BIGINT (FK) | User who received the profile card |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_friendships_sender` on `sender_id`
- `idx_friendships_recipient` on `recipient_id`
- Unique index on `(sender_id, recipient_id)`

**Foreign Keys:**
- `sender_id` → `users(id)` ON DELETE CASCADE
- `recipient_id` → `users(id)` ON DELETE CASCADE

**Usage Pattern:**
- Query by `recipient_id` to find profile cards received by a user
- Query by `sender_id` to find profile cards sent by a user

### 5. Events

Quiz event entities with lifecycle management.

**Table: `events`**

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK, IDENTITY) | Auto-generated primary key |
| initiator_id | BIGINT (FK) | User who initiated the event |
| status | VARCHAR(50) | Event status (created, active, ended, expired, deleted) |
| meta | JSONB | Flexible event metadata |
| expires_at | TIMESTAMP | Event expiration time |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_events_initiator` on `initiator_id`
- `idx_events_status` on `status`
- `idx_events_expires_at` on `expires_at`
- `idx_events_created_at` on `created_at DESC`

**Foreign Keys:**
- `initiator_id` → `users(id)` ON DELETE CASCADE

**Status Values:**
- `created`: Event has been created but not started
- `active`: Event is ongoing
- `ended`: Event has ended normally
- `expired`: Event has expired
- `deleted`: Event is deleted

### 6. Event Invitation Codes

Event invitation codes, separated for performance.

**Table: `event_invitation_codes`**

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK, IDENTITY) | Auto-generated primary key |
| event_id | BIGINT (FK) | Reference to events table |
| invitation_code | VARCHAR(64) | Invitation code for the event |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_event_codes_event_id` on `event_id`
- `idx_event_codes_code` on `invitation_code`
- Unique index on `(event_id, invitation_code)`

**Foreign Keys:**
- `event_id` → `events(id)` ON DELETE CASCADE

**Notes:**
- Invitation codes should be unique among non-expired/non-deleted events
- Enforced at application level with exclusive locking when generating codes

### 7. Event Attendees

Tracks user participation in events.

**Table: `event_attendees`**

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK, IDENTITY) | Auto-generated primary key |
| event_id | BIGINT (FK) | Reference to events table |
| attendee_user_id | BIGINT (FK) | User attending the event |
| meta | JSONB | Flexible attendee metadata |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_event_attendees_event` on `event_id`
- `idx_event_attendees_user` on `attendee_user_id`
- `idx_event_attendees_created_at` on `created_at DESC`
- Unique index on `(event_id, attendee_user_id)`

**Foreign Keys:**
- `event_id` → `events(id)` ON DELETE CASCADE
- `attendee_user_id` → `users(id)` ON DELETE CASCADE

## Entity Relationships

```
users (1) ←──→ (N) authn_providers
users (1) ←──→ (N) user_profiles
users (1) ←──current_profile_revision── user_profiles
users (N) ←──→ (N) friendships (sender ↔ recipient)
users (1) ←──→ (N) events (as initiator)
events (1) ←──→ (N) event_invitation_codes
events (1) ←──→ (N) event_attendees
users (1) ←──→ (N) event_attendees (as attendee)
```

## MyBatis Mappers

Each entity has a corresponding MyBatis mapper interface:

- `UserMapper`: CRUD operations for users
- `AuthnProviderMapper`: Authentication provider management
- `UserProfileMapper`: Profile revision management
- `FriendshipMapper`: Friendship relationship management
- `EventMapper`: Event management
- `EventInvitationCodeMapper`: Invitation code management
- `EventAttendeeMapper`: Attendee management

## JSONB Fields

Several tables use JSONB for flexible metadata storage:

- `users.meta`: Store pause reasons, administrative notes
- `user_profiles.profile_data`: Store user profile information
- `user_profiles.revision_meta`: Store revision notes
- `events.meta`: Store event-specific metadata
- `event_attendees.meta`: Store attendee-specific metadata

**Benefits of JSONB:**
- Schema flexibility without migrations
- Efficient storage and indexing
- Native PostgreSQL JSON operators support
- Type-safe in application code with Java objects

## Migration Notes

This is a V1 migration that replaces the previous schema completely:
- Old `rooms` table replaced with `events`
- Old inline authentication replaced with separate `authn_providers` table
- New profile management with revision history
- New social features with friendships
- All primary keys use `BIGINT GENERATED ALWAYS AS IDENTITY`
- Comprehensive indexes for performance
- Proper foreign key constraints with CASCADE deletes
