CREATE TABLE refunds (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID          NOT NULL,
    event_id     UUID          NOT NULL,
    customer_id  UUID          NOT NULL,
    user_email   VARCHAR(255)  NOT NULL,
    user_name    VARCHAR(255)  NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    reason       VARCHAR(255)  NOT NULL DEFAULT 'EVENTO_CANCELADO',
    status       VARCHAR(50)   NOT NULL DEFAULT 'REQUESTED',
    requested_at TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_refunds_order_id    ON refunds(order_id);
CREATE INDEX idx_refunds_customer_id ON refunds(customer_id);
CREATE INDEX idx_refunds_status      ON refunds(status);
