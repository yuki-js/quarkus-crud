-- Initial database schema for Quarkus CRUD application
-- This migration creates all necessary tables and indexes for the application

-- ============================================================
-- User Entity
-- Basic user entity handling user fundamental information only
-- ============================================================
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Account lifecycle status
    lifecycle_status VARCHAR(50) NOT NULL DEFAULT 'created',
    -- Current profile card revision (foreign key to user_profiles)
    current_profile_card_revision BIGINT,
    -- Metadata in JSONB format (e.g., pause reason)
    meta JSONB DEFAULT '{}'::jsonb
);

COMMENT ON TABLE users IS 'Basic user entity - handles user fundamental information';
COMMENT ON COLUMN users.lifecycle_status IS 'Account lifecycle: created, provisioned, active, paused, deleted';
COMMENT ON COLUMN users.current_profile_card_revision IS 'Current profile card revision ID (cached from user_profiles)';
COMMENT ON COLUMN users.meta IS 'Metadata like pause reason, stored as JSONB';

CREATE INDEX idx_users_lifecycle ON users(lifecycle_status);

-- ============================================================
-- Authentication Providers
-- Authentication provider information for users
-- ============================================================
CREATE TABLE authn_providers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    auth_method VARCHAR(50) NOT NULL,
    auth_identifier VARCHAR(255),
    external_subject VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_authn_providers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE authn_providers IS 'Authentication provider information for users';
COMMENT ON COLUMN authn_providers.auth_method IS 'Authentication method: anonymous, oidc';
COMMENT ON COLUMN authn_providers.auth_identifier IS 'Internal authentication identifier (UUID for anonymous)';
COMMENT ON COLUMN authn_providers.external_subject IS 'Subject identifier from external provider (NULL for anonymous)';

CREATE INDEX idx_authn_providers_user_id ON authn_providers(user_id);
CREATE UNIQUE INDEX idx_authn_providers_auth_identifier ON authn_providers(auth_identifier) WHERE auth_identifier IS NOT NULL;
CREATE INDEX idx_authn_providers_auth_method_subject ON authn_providers(auth_method, external_subject);

-- ============================================================
-- User Profile
-- User profile card information (immutable revision history)
-- ============================================================
CREATE TABLE user_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    profile_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revision_meta JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE user_profiles IS 'User profile card information - each record is immutable (except revision_meta)';
COMMENT ON COLUMN user_profiles.profile_data IS 'Profile data stored as JSONB';
COMMENT ON COLUMN user_profiles.revision_meta IS 'Revision metadata (mutable)';

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_created_at ON user_profiles(user_id, created_at DESC);

-- Add foreign key for current_profile_card_revision after user_profiles table is created
ALTER TABLE users ADD CONSTRAINT fk_users_current_profile 
    FOREIGN KEY (current_profile_card_revision) REFERENCES user_profiles(id) ON DELETE SET NULL;

-- ============================================================
-- Friendship
-- Unidirectional m:n connection between users (profile card exchanges)
-- ============================================================
CREATE TABLE friendships (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_user_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_friendships_sender FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_friendships_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_friendships_not_self CHECK (sender_user_id != recipient_user_id)
);

COMMENT ON TABLE friendships IS 'Unidirectional connections between users who exchanged profile cards';
COMMENT ON COLUMN friendships.sender_user_id IS 'User who sent their profile card';
COMMENT ON COLUMN friendships.recipient_user_id IS 'User who received the profile card';

-- Unique constraint: one friendship per sender-recipient pair
CREATE UNIQUE INDEX idx_friendships_sender_recipient ON friendships(sender_user_id, recipient_user_id);
-- Index for reverse lookup (finding who sent cards to me)
CREATE INDEX idx_friendships_recipient ON friendships(recipient_user_id);

-- ============================================================
-- Event
-- Quiz event entity (formerly "room")
-- ============================================================
CREATE TABLE events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    initiator_user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'created',
    meta JSONB DEFAULT '{}'::jsonb,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_initiator FOREIGN KEY (initiator_user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE events IS 'Quiz event entity';
COMMENT ON COLUMN events.initiator_user_id IS 'User who initiated the event';
COMMENT ON COLUMN events.status IS 'Event status: created, active, ended, expired, deleted';
COMMENT ON COLUMN events.expires_at IS 'Expiration time for the event';

CREATE INDEX idx_events_initiator ON events(initiator_user_id);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_expires_at ON events(expires_at);

-- ============================================================
-- Event Invitation Code
-- Event participation codes (separated for performance)
-- ============================================================
CREATE TABLE event_invitation_codes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL,
    invitation_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_codes_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

COMMENT ON TABLE event_invitation_codes IS 'Event participation codes - separated table for performance';
COMMENT ON COLUMN event_invitation_codes.invitation_code IS 'Participation code (unique among non-expired/deleted events)';

-- Invitation code must be unique among active events
-- We enforce this with a partial unique index (only for non-expired/deleted events)
CREATE INDEX idx_event_codes_event ON event_invitation_codes(event_id);
CREATE INDEX idx_event_codes_code ON event_invitation_codes(invitation_code);

-- ============================================================
-- Event Attendee
-- Quiz event participants
-- ============================================================
CREATE TABLE event_attendees (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL,
    attendee_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    meta JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT fk_event_attendees_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_attendees_user FOREIGN KEY (attendee_user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE event_attendees IS 'Quiz event participants';

-- Unique constraint: one attendance record per user per event
CREATE UNIQUE INDEX idx_event_attendees_event_user ON event_attendees(event_id, attendee_user_id);
CREATE INDEX idx_event_attendees_user ON event_attendees(attendee_user_id);
