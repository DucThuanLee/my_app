-- Add column to track when a notification starts processing
-- This is used to detect stuck SENDING notifications
-- and allow re-claim after a timeout.

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMP;

-- Optional but recommended index:
-- Helps reclaim stuck SENDING notifications efficiently
CREATE INDEX IF NOT EXISTS idx_notifications_processing_started_at
    ON notifications (processing_started_at);