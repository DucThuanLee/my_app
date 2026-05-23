-- V13__add_paypal_fields_to_orders_and_refunds.sql

-- ===== ORDERS: PayPal fields =====
ALTER TABLE orders
    ADD COLUMN paypal_order_id VARCHAR(255),
    ADD COLUMN paypal_capture_id VARCHAR(255);

CREATE UNIQUE INDEX ux_orders_paypal_order_id
    ON orders(paypal_order_id)
    WHERE paypal_order_id IS NOT NULL;

CREATE UNIQUE INDEX ux_orders_paypal_capture_id
    ON orders(paypal_capture_id)
    WHERE paypal_capture_id IS NOT NULL;


-- ===== REFUNDS: allow multi-provider refunds =====
ALTER TABLE refunds
    ALTER COLUMN stripe_refund_id DROP NOT NULL;

ALTER TABLE refunds
    ADD COLUMN paypal_refund_id VARCHAR(255),
    ADD COLUMN paypal_capture_id VARCHAR(255);

CREATE UNIQUE INDEX ux_refunds_paypal_refund_id
    ON refunds(paypal_refund_id)
    WHERE paypal_refund_id IS NOT NULL;

CREATE INDEX idx_refunds_paypal_capture_id
    ON refunds(paypal_capture_id);