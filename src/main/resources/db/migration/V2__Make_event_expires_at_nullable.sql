-- Migration to make events.expires_at nullable
-- This aligns the database schema with the OpenAPI specification which defines expiresAt as nullable

ALTER TABLE events ALTER COLUMN expires_at DROP NOT NULL;

-- Update the comment to reflect the nullable nature
COMMENT ON COLUMN events.expires_at IS 'Optional expiration timestamp for the event (null means no expiration)';
