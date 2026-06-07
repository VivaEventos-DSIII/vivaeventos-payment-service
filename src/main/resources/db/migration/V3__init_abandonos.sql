CREATE TABLE abandonos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID NOT NULL REFERENCES payments(id),
    order_id        UUID NOT NULL,
    customer_email  VARCHAR(255),
    amount_in_cents BIGINT,
    minutes_pending INTEGER,
    detected_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_abandonos_payment_id    ON abandonos(payment_id);
CREATE INDEX idx_abandonos_order_id      ON abandonos(order_id);
CREATE INDEX idx_abandonos_customer_email ON abandonos(customer_email);
CREATE INDEX idx_abandonos_detected_at   ON abandonos(detected_at);
