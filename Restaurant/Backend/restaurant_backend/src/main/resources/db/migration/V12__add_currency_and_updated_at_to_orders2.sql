-- ===== 1. Add new columns (nullable first) =====
ALTER TABLE orders
    ADD COLUMN currency VARCHAR(3),
ADD COLUMN updated_at TIMESTAMP;

-- ===== 2. Backfill existing data =====
UPDATE orders
SET currency = 'EUR'
WHERE currency IS NULL;

UPDATE orders
SET updated_at = created_at
WHERE updated_at IS NULL;

-- ===== 3. Add constraints =====
ALTER TABLE orders
    ALTER COLUMN currency SET NOT NULL,
ALTER COLUMN updated_at SET NOT NULL;

-- ===== 4. Optional: default values =====
ALTER TABLE orders
    ALTER COLUMN currency SET DEFAULT 'EUR',
ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;