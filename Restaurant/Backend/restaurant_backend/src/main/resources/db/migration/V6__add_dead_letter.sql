-- V6: Dead-letter support for notifications
-- This migration adds:
-- 1) DEAD status (dead-lettered)
-- 2) dead_lettered_at timestamp
-- 3) dead_letter_reason short text
-- 4) helpful indexes for monitoring / querying dead letters

-- 1) Add columns (nullable)
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS dead_lettered_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS dead_letter_reason VARCHAR(500) NULL;

-- 2) Optional: widen status column in case it's too small (safe)
-- If your status column is VARCHAR(20) this is already enough, but keeping it safe:
ALTER TABLE notifications
ALTER COLUMN status TYPE VARCHAR(30);

-- 3) Index for dead letters
CREATE INDEX IF NOT EXISTS idx_notifications_dead_lettered_at
    ON notifications (dead_lettered_at);

-- 4) Optional: index for status + created_at (monitoring)
CREATE INDEX IF NOT EXISTS idx_notifications_status_created
    ON notifications (status, created_at);