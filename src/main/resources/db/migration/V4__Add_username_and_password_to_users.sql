-- Add username and password fields for JWT authentication
-- Username is optional to maintain backward compatibility with guest users
ALTER TABLE users ADD COLUMN username VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN roles VARCHAR(255) DEFAULT 'user';

-- Create index on username for faster lookups
CREATE INDEX idx_users_username ON users(username);

-- Make guest_token nullable since users with username won't need it
ALTER TABLE users ALTER COLUMN guest_token DROP NOT NULL;

-- Add constraint: either username or guest_token must be present
ALTER TABLE users ADD CONSTRAINT check_user_identity 
    CHECK ((username IS NOT NULL) OR (guest_token IS NOT NULL));
