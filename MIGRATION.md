# Data Model Migration Summary

## What Was Done

This PR implements the complete data model refactoring as specified in the issue. The migration is a **breaking change** that replaces the existing schema from V1.

### New Data Model

#### 1. **Users Table** (Refactored)
- Removed authentication fields (moved to separate table)
- Added `account_lifecycle` enum (created, provisioned, active, paused, deleted)
- Added `current_profile_revision` FK to user_profiles
- Added `meta` JSONB for flexible metadata

#### 2. **Authentication Providers Table** (New)
- Separate table for authentication methods
- Supports multiple auth methods per user
- Fields: user_id, auth_method, auth_identifier, external_subject
- Unique constraints for anonymous and external auth

#### 3. **User Profiles Table** (New)
- Immutable profile revisions
- JSONB profile_data for flexibility
- Mutable revision_meta for administrative purposes
- Latest profile cached in users.current_profile_revision

#### 4. **Friendships Table** (New)
- Unidirectional m:n relationship
- sender_id → recipient_id mapping
- Supports profile card exchange pattern
- Unique constraint per pair

#### 5. **Events Table** (Replaces Rooms)
- Quiz event entity
- Status lifecycle: created, active, ended, expired, deleted
- expires_at timestamp for lifecycle management
- JSONB meta for flexible event data

#### 6. **Event Invitation Codes Table** (New)
- Separated for performance
- Supports code reuse after expiration/deletion
- Application-level uniqueness enforcement with locking

#### 7. **Event Attendees Table** (New)
- Many-to-many between events and users
- JSONB meta for attendee-specific data
- Unique constraint per event-user pair

### Database Features

- All PKs: `BIGINT GENERATED ALWAYS AS IDENTITY`
- JSONB fields for flexible metadata
- Comprehensive indexes for performance
- Proper foreign key constraints with CASCADE
- Partial indexes where applicable

### Code Changes

#### Entity Classes
- Updated: `User.java`
- New: `AccountLifecycle.java`, `AuthMethod.java`, `AuthnProvider.java`
- New: `UserProfile.java`, `Friendship.java`
- New: `Event.java`, `EventStatus.java`, `EventInvitationCode.java`, `EventAttendee.java`
- Kept: `Room.java` (for backward compatibility during transition)

#### MyBatis Mappers
- Updated: `UserMapper.java`
- New: `AuthnProviderMapper.java`, `UserProfileMapper.java`, `FriendshipMapper.java`
- New: `EventMapper.java`, `EventInvitationCodeMapper.java`, `EventAttendeeMapper.java`
- Kept: `RoomMapper.java` (for backward compatibility during transition)

#### Services
- Updated: `UserService.java` - works with User + AuthnProvider
- Updated: `JwtService.java` - fetches auth info from AuthnProvider table
- Updated: `AuthenticationService.java` - uses AuthMethod enum
- Kept: `RoomService.java` (for backward compatibility during transition)

### CI Validation Status

✅ **Passing Checks:**
- OpenAPI model generation
- Code formatting (spotlessCheck)
- Code quality/linting (checkstyleMain)
- OpenAPI spec validation (Spectral)
- OpenAPI spec validation (OpenAPI Generator CLI)
- Database schema migration (manually validated)
- Main code compilation

⚠️ **Expected Failures:**
- Test compilation (tests reference old User fields)
- Test execution (if tests could compile)

**This is acceptable per issue instructions:**
> "データモデルを変更したことに伴い新たに発生したテストはこけても放置してもかまわない。後方互換性はすべて捨ててしまってもかまわない。"
> 
> Translation: "Tests that fail due to data model changes can be left broken. All backward compatibility can be discarded."

## Migration Path

### Database Migration
- Flyway V1 migration completely replaces old schema
- No migration from old data (all existing data will be lost)
- Fresh start from V1 as specified in issue

### Code Migration
The old Room-based code still exists for reference but:
- New Event-based entities and mappers are ready
- Services updated to use new User/AuthnProvider model
- Applications should migrate to Event model going forward

### Breaking Changes
- User entity API completely changed
- Authentication now in separate table
- Room → Event entity change
- All tests need to be rewritten for new model

## Next Steps

1. **Update Tests** (future work)
   - Rewrite UserServiceTest for new User/AuthnProvider model
   - Rewrite integration tests for new schema
   - Update all other test files

2. **Migrate Room-based Code** (future work)
   - Update RoomService to use Event entities
   - Update RoomsApiImpl to use Event entities
   - Remove old Room entity when migration complete

3. **Add Business Logic** (future work)
   - Profile revision management
   - Friendship operations
   - Event invitation code generation with locking
   - Event lifecycle management

## Documentation

- **Schema Documentation**: `docs/data-model.md`
  - All table schemas
  - Entity relationships diagram
  - JSONB usage patterns
  - MyBatis mapper overview

## Testing Evidence

### Schema Validation
Successfully created and tested:
```sql
-- All 7 tables created
✓ users
✓ authn_providers
✓ user_profiles
✓ friendships
✓ events
✓ event_invitation_codes
✓ event_attendees

-- Basic CRUD operations tested
✓ INSERT user
✓ INSERT authn_provider
✓ INSERT user_profile
✓ SELECT with JOIN
```

### Code Quality
- All new code follows project style guidelines
- Proper Javadoc documentation
- Consistent naming conventions
- Type-safe enum handling
