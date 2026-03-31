-- ============================================
-- V8: Add refund-related fields to orders table
-- ============================================

-- 1. refund_status
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS refund_status VARCHAR(50);

UPDATE orders
SET refund_status = 'NONE'
WHERE refund_status IS NULL;

ALTER TABLE orders
    ALTER COLUMN refund_status SET NOT NULL;

ALTER TABLE orders
    ALTER COLUMN refund_status SET DEFAULT 'NONE';


-- 2. refunded_amount (for partial refunds)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(12,2);

-- Optional: init to 0 instead of NULL
UPDATE orders
SET refunded_amount = 0
WHERE refunded_amount IS NULL;


-- 3. refunded_at (timestamp when refund completed)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMP;

-- (no default needed, keep NULL until refund happens)