# ProfileMissing: Semi-Normal Response Handling

## Overview

The `ProfileMissing` response type is a specialized HTTP 404 response that distinguishes between a "profile not created yet" state (semi-normal) and actual error conditions. This allows clients—especially auto-generated clients and AI systems—to programmatically distinguish between these cases and handle them appropriately.

## Background

Previously, when a user existed but had not created a profile, the API returned a generic `ErrorResponse` with a 404 status code:

```json
{
  "error": "Profile not found"
}
```

This was problematic because:
1. It conflated "profile not created" (a normal state) with actual errors
2. Auto-generated clients treated all 404s uniformly as errors
3. AI systems couldn't distinguish between "missing profile" and "real error"

## Solution: RFC 7807 Problem Details

The `ProfileMissing` response follows RFC 7807 Problem Details for HTTP APIs, providing a structured, machine-readable format:

```json
{
  "type": "about:blank",
  "title": "Profile Not Created",
  "status": 404,
  "code": "PROFILE_MISSING",
  "detail": "The user has not created a profile yet."
}
```

The key field is `code: "PROFILE_MISSING"`, which allows clients to programmatically detect this semi-normal condition.

## Affected Endpoints

### `/api/me/profile` (GET)
When the authenticated user has not created a profile:
- **Status**: 404
- **Body**: `ProfileMissing`
- **Content-Type**: `application/json`

### `/api/users/{userId}/profile` (GET)
When the specified user exists but has not created a profile:
- **Status**: 404
- **Body**: `ProfileMissing`
- **Content-Type**: `application/json`

## Client Implementation Guide

### TypeScript/JavaScript Example

```typescript
import { ProfilesApi, ProfileMissing, UserProfile } from '@yuki-js/quarkus-crud-js-fetch-client';

async function getUserProfile(userId: number): Promise<UserProfile | null> {
  try {
    const response = await profilesApi.getUserProfile({ userId });
    return response;
  } catch (error) {
    // Check if this is a ProfileMissing case
    if (error.status === 404 && error.body?.code === 'PROFILE_MISSING') {
      // This is normal - profile not created yet
      return null;
    }
    // This is an actual error - rethrow
    throw error;
  }
}
```

### Java Example

```java
Response response = client.target("/api/users/{userId}/profile")
    .resolveTemplate("userId", userId)
    .request()
    .get();

if (response.getStatus() == 404) {
    ProfileMissing missing = response.readEntity(ProfileMissing.class);
    if (missing.getCode() == ProfileMissing.CodeEnum.PROFILE_MISSING) {
        // Normal case: profile not created yet
        return Optional.empty();
    }
}
// Handle other cases...
```

### AI/LLM Integration

When integrating with AI systems, configure the system prompt to recognize `PROFILE_MISSING`:

```
When you receive a 404 response with code="PROFILE_MISSING", 
treat this as a normal "profile not created" state, NOT an error.
Return null or an appropriate empty state to the user.
```

## Migration Notes

### Breaking Changes
⚠️ **IMPORTANT**: If your client code directly parses 404 responses from Profile endpoints as `ErrorResponse`, you must update it to handle `ProfileMissing`.

**Before:**
```typescript
// This will break!
const error: ErrorResponse = response.error;
console.log(error.error); // undefined
```

**After:**
```typescript
if (response.status === 404 && response.body?.code === 'PROFILE_MISSING') {
  // Handle as normal missing profile
  return null;
}
```

### Backward Compatibility
- The HTTP status code remains 404 (no change)
- Response body structure is different (breaking change for clients that parse the body)
- Clients that ignore 404 response bodies are unaffected

### Recommended Migration Path
1. Update client code to check for `code === 'PROFILE_MISSING'`
2. Fall back to treating any 404 as "profile missing" if the code check fails (temporary compatibility)
3. Once all clients are updated, remove fallback logic

## OpenAPI Specification

The `ProfileMissing` schema is defined in `openapi/components/schemas/common.yaml`:

```yaml
ProfileMissing:
  type: object
  description: >
    Semi-normal response indicating that a profile has not been created yet.
  required:
    - type
    - title
    - status
    - code
  properties:
    type:
      type: string
      example: about:blank
    title:
      type: string
      example: Profile Not Created
    status:
      type: integer
      example: 404
    code:
      type: string
      enum: [PROFILE_MISSING]
      example: PROFILE_MISSING
    detail:
      type: string
      example: The user has not created a profile yet.
```

## Testing

Integration tests verify the ProfileMissing behavior:
- `ProfileMissingIntegrationTest.testGetMyProfile_WhenProfileNotCreated_ReturnsProfileMissing()`
- `ProfileMissingIntegrationTest.testGetUserProfile_WhenProfileNotCreated_ReturnsProfileMissing()`

Run tests:
```bash
./gradlew test --tests ProfileMissingIntegrationTest
```

## Future Extensions

This pattern can be extended to other semi-normal cases:
- User settings not configured → `SETTINGS_MISSING`
- User preferences not set → `PREFERENCES_MISSING`
- Avatar not uploaded → `AVATAR_MISSING`

Each would follow the same RFC 7807 structure with a unique `code` value.

## References
- [RFC 7807: Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)
- [OpenAPI Schema Guide](./openapi-schema.md)
- [Client Integration Guide](./clients.md)
