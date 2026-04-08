CREATE TABLE refunds (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- ===== RELATION =====
                         order_id UUID NOT NULL,

    -- ===== STRIPE =====
                         stripe_refund_id VARCHAR(255) NOT NULL,
                         stripe_charge_id VARCHAR(255),

    -- ===== MONEY =====
                         amount NUMERIC(12,2) NOT NULL,

    -- ===== STATUS =====
                         status VARCHAR(30) NOT NULL, -- StripeRefundStatus

    -- ===== OPTIONAL =====
                         reason VARCHAR(50),
                         failure_reason VARCHAR(255),

    -- ===== AUDIT =====
                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                         updated_at TIMESTAMP

);

-- ===== CONSTRAINTS =====
ALTER TABLE refunds
    ADD CONSTRAINT fk_refund_order
        FOREIGN KEY (order_id)
            REFERENCES orders(id)
            ON DELETE CASCADE;

-- Idempotency guarantee (CRITICAL)
ALTER TABLE refunds
    ADD CONSTRAINT uk_stripe_refund_id UNIQUE (stripe_refund_id);

-- ===== INDEXES =====

-- Query by order (very common)
CREATE INDEX idx_refunds_order_id ON refunds(order_id);

-- Fast lookup by stripe id
CREATE INDEX idx_refunds_stripe_refund_id ON refunds(stripe_refund_id);

-- Aggregate optimization (VERY IMPORTANT)
CREATE INDEX idx_refunds_order_status
    ON refunds(order_id, status);