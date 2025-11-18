-- Add columns to support multiple authentication providers
-- This enables seamless integration of external OpenID Connect providers

-- Track which authentication provider was used
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(50) NOT NULL DEFAULT 'anonymous';

-- Store the subject from external authentication providers (e.g., OIDC sub claim)
-- For anonymous users, this will be NULL as they use auth_identifier
ALTER TABLE users ADD COLUMN external_subject VARCHAR(255);

-- Create index for efficient lookups by external provider and subject
CREATE INDEX idx_users_provider_external_subject ON users(auth_provider, external_subject);

-- Update comments
COMMENT ON COLUMN users.auth_provider IS 'Authentication provider type: anonymous, oidc, etc.';
COMMENT ON COLUMN users.external_subject IS 'Subject identifier from external authentication provider (NULL for anonymous users)';
COMMENT ON COLUMN users.auth_identifier IS 'Internal authentication identifier (UUID) - used for anonymous auth and as internal reference';
