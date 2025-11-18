-- Initial database schema for Quarkus CRUD application
-- This migration creates all necessary tables and indexes for the application

-- Users table - all users are equal regardless of authentication method
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    auth_identifier VARCHAR(255) UNIQUE NOT NULL,
    auth_provider VARCHAR(50) NOT NULL DEFAULT 'anonymous',
    external_subject VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Comments for documentation
COMMENT ON TABLE users IS 'Users table - all users are equal regardless of authentication method';
COMMENT ON COLUMN users.auth_identifier IS 'Internal authentication identifier (UUID) - used for anonymous auth and as internal reference';
COMMENT ON COLUMN users.auth_provider IS 'Authentication provider type: anonymous, oidc, etc.';
COMMENT ON COLUMN users.external_subject IS 'Subject identifier from external authentication provider (NULL for anonymous users)';

-- Indexes on users table
CREATE INDEX idx_users_auth_identifier ON users(auth_identifier);
CREATE INDEX idx_users_provider_external_subject ON users(auth_provider, external_subject);

-- Rooms table with foreign key to users
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rooms_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes on rooms table
CREATE INDEX idx_rooms_user_id ON rooms(user_id);
CREATE INDEX idx_rooms_created_at ON rooms(created_at DESC);
