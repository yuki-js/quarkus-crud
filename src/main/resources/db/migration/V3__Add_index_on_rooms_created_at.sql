-- Add index on rooms.created_at for better performance on queries ordering by created_at
CREATE INDEX idx_rooms_created_at ON rooms(created_at DESC);
