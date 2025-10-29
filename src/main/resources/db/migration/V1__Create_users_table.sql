-- Create users table for anonymous guest authentication
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    guest_token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on guest_token for faster lookups
CREATE INDEX idx_users_guest_token ON users(guest_token);
