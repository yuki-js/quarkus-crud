-- Rename guest_token column to auth_identifier to reflect that users are equal
-- and authentication method is just a property
ALTER TABLE users RENAME COLUMN guest_token TO auth_identifier;

-- Update index name to reflect the new column name
DROP INDEX IF EXISTS idx_users_guest_token;
CREATE INDEX idx_users_auth_identifier ON users(auth_identifier);

-- Update comments to reflect the new philosophy
COMMENT ON TABLE users IS 'Users table - all users are equal regardless of authentication method';
COMMENT ON COLUMN users.auth_identifier IS 'Authentication identifier (UUID) - used for anonymous auth and as reference for external auth providers';
