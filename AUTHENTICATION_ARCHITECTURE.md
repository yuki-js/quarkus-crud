# Multi-Provider Authentication Architecture

This document explains the authentication architecture and how to integrate external OpenID Connect (OIDC) providers with minimal code changes.

## Architecture Overview

The authentication system is designed to support multiple authentication providers equally:
- **Anonymous authentication**: Users created locally without credentials (current implementation)
- **External OIDC**: Users authenticated via external providers (future integration)

All users are equal entities. Authentication provider is a property, not a user type.

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    auth_identifier VARCHAR(255) UNIQUE NOT NULL,  -- Internal UUID reference for all users
    auth_provider VARCHAR(50) NOT NULL DEFAULT 'anonymous',  -- Provider type: anonymous, oidc
    external_subject VARCHAR(255),  -- Subject from external provider (NULL for anonymous)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_auth_identifier ON users(auth_identifier);
CREATE INDEX idx_users_provider_external_subject ON users(auth_provider, external_subject);
```

### User Identity Model

- **Anonymous users**: 
  - `auth_identifier`: Generated UUID (primary identity)
  - `auth_provider`: "anonymous"
  - `external_subject`: NULL
  - JWT subject: `auth_identifier`

- **External OIDC users**:
  - `auth_identifier`: Generated UUID (internal reference)
  - `auth_provider`: "oidc"
  - `external_subject`: Subject from OIDC provider (e.g., Google user ID)
  - JWT subject: `external_subject`

## Service Layer

### JwtService

**Purpose**: Generates JWT tokens for internally-authenticated users (anonymous).

**Note**: When using external OIDC, JWT tokens come from the external provider - this service is NOT used.

```java
// For anonymous users only
String token = jwtService.generateAnonymousToken(user);
```

### UserService

**Purpose**: Manages user lifecycle for all authentication providers.

**Key Methods**:

```java
// Create anonymous user
User user = userService.createAnonymousUser();

// Get or create OIDC user (creates if doesn't exist)
User user = userService.getOrCreateExternalUser(
    AuthenticationProvider.OIDC, 
    oidcSubject
);

// Find by internal identifier
Optional<User> user = userService.findByAuthIdentifier(uuid);

// Find by external provider and subject
Optional<User> user = userService.findByProviderAndExternalSubject(
    AuthenticationProvider.OIDC, 
    oidcSubject
);
```

### AuthenticationService

**Purpose**: Centralizes authentication logic with provider-specific strategies.

**Current Implementation**: Supports anonymous and has placeholder for OIDC.

```java
// Automatically determines provider and looks up user
Optional<User> user = authenticationService.authenticateFromJwt(jwt);
```

**Provider Detection Logic**:
1. Checks JWT `groups` claim for provider type
2. For anonymous: group = "anonymous"
3. For OIDC: group = "oidc" OR check issuer claim
4. Routes to appropriate lookup strategy

## Integrating External OIDC Provider

### Step 1: Configure OIDC Provider

Add OIDC configuration to `application.properties`:

```properties
# Accept tokens from external OIDC provider
mp.jwt.verify.publickey.location=https://accounts.google.com/.well-known/jwks.json
mp.jwt.verify.issuer=https://accounts.google.com

# Accept both our tokens and Google's tokens
quarkus.smallrye-jwt.enabled=true
```

### Step 2: Update AuthenticationService

Uncomment and implement the provider detection logic:

```java
private AuthenticationProvider determineProviderFromJwt(JsonWebToken jwt) {
    // Check groups claim
    if (jwt.getGroups() != null && jwt.getGroups().contains("anonymous")) {
        return AuthenticationProvider.ANONYMOUS;
    }
    
    // Check issuer for external OIDC
    String issuer = jwt.getIssuer();
    if (issuer != null) {
        if (issuer.equals("https://accounts.google.com")) {
            return AuthenticationProvider.OIDC;
        }
        // Add more OIDC providers as needed
    }
    
    return AuthenticationProvider.ANONYMOUS;
}
```

### Step 3: Add OIDC Authentication Endpoint (Optional)

If you want to manually handle OIDC token exchange:

```java
@POST
@Path("/oidc/token")
public Response authenticateOidc(@HeaderParam("Authorization") String authHeader) {
    // Extract and validate OIDC token
    String oidcToken = authHeader.substring("Bearer ".length());
    
    // Parse JWT (Quarkus SmallRye JWT does this automatically)
    // The jwt will be injected and validated
    
    // Get or create user
    String externalSubject = jwt.getSubject();
    User user = userService.getOrCreateExternalUser(
        AuthenticationProvider.OIDC,
        externalSubject
    );
    
    // Return user info
    return Response.ok(toUserResponse(user)).build();
}
```

### Step 4: Test OIDC Integration

The existing authentication filter will automatically work with OIDC tokens because:
1. SmallRye JWT validates the token signature
2. `AuthenticationService.authenticateFromJwt()` detects the provider
3. User is looked up by `(auth_provider, external_subject)`
4. If not found, `getOrCreateExternalUser()` creates the user

## Code Changes Required for OIDC Integration

### Minimal Changes Needed:

1. **Configuration** (`application.properties`):
   - Add OIDC provider's JWKS URL
   - Add accepted issuer

2. **AuthenticationService** (1 method):
   - Update `determineProviderFromJwt()` to detect OIDC issuer

3. **Optional**: Add OIDC-specific endpoints if needed

### No Changes Needed:

- ✅ User entity
- ✅ UserMapper
- ✅ UserService (methods already exist)
- ✅ AuthenticationFilter
- ✅ Database schema
- ✅ JWT validation logic

## Testing

### Anonymous Authentication

```bash
# Create anonymous user
curl -X POST http://localhost:8080/api/auth/guest

# Returns:
{
  "id": 1,
  "createdAt": "2024-01-01T00:00:00Z"
}

# Header:
Authorization: Bearer eyJhbGciOiJFUzI1NiJ9...
```

### OIDC Authentication (Future)

```bash
# Use OIDC token from external provider
curl -H "Authorization: Bearer <google_oidc_token>" \
     http://localhost:8080/api/auth/me

# System automatically:
# 1. Validates token against Google's JWKS
# 2. Extracts subject from token
# 3. Looks up or creates user
# 4. Returns user info
```

## Architecture Benefits

1. **Separation of Concerns**: Authentication providers are separate from user management
2. **Equal Treatment**: All users are equal regardless of provider
3. **Minimal Changes**: OIDC integration requires ~10 lines of code
4. **No Migration**: Existing anonymous users work unchanged
5. **Extensibility**: Easy to add more providers (GitHub, Auth0, etc.)
6. **Stateless**: JWT tokens enable horizontal scaling

## Provider Comparison

| Aspect | Anonymous | External OIDC |
|--------|-----------|---------------|
| Token Issuer | This service | External provider |
| Token Generation | JwtService | External provider |
| User Creation | On-demand (guest endpoint) | First authentication |
| Subject | auth_identifier (UUID) | external_subject (provider's ID) |
| Lookup | By auth_identifier | By (provider, external_subject) |
| Groups | "anonymous" | "oidc" or provider-specific |

## Future Enhancements

1. **Multiple OIDC Providers**: Support Google, GitHub, Auth0, etc.
2. **Token Refresh**: Implement refresh token logic for long sessions
3. **Profile Sync**: Sync user profile data from OIDC provider
4. **Migration Path**: Allow anonymous users to link OIDC accounts
5. **Admin API**: Endpoints to manage user accounts

## Security Considerations

1. **Token Validation**: Always validate JWT signature and expiration
2. **Issuer Whitelist**: Only accept tokens from trusted issuers
3. **Key Rotation**: Support for external provider key rotation via JWKS
4. **Audit Logging**: Log authentication events for security monitoring
5. **Rate Limiting**: Prevent abuse of authentication endpoints
