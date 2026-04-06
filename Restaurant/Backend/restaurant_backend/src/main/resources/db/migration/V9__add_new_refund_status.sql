-- add new column
ALTER TABLE orders ADD COLUMN stripe_refund_status VARCHAR(30);

-- set default cho business status
UPDATE orders
SET refund_status = 'NOT_REQUESTED'
WHERE refund_status = 'NONE';