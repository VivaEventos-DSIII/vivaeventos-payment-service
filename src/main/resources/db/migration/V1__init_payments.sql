CREATE TABLE payments (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id              UUID NOT NULL,
    ticket_type_id        UUID,
    wompi_transaction_id  VARCHAR(255),
    reference             VARCHAR(255) NOT NULL UNIQUE,
    amount_in_cents       BIGINT NOT NULL,
    currency              VARCHAR(10)  NOT NULL DEFAULT 'COP',
    status                VARCHAR(50)  NOT NULL DEFAULT 'PENDIENTE',
    payment_method_type   VARCHAR(100),
    customer_email        VARCHAR(255) NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE payment_webhooks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id  UUID REFERENCES payments(id),
    event_type  VARCHAR(100) NOT NULL,
    raw_payload JSONB        NOT NULL,
    processed   BOOLEAN      NOT NULL DEFAULT FALSE,
    received_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id   ON payments(order_id);
CREATE INDEX idx_payments_reference  ON payments(reference);
CREATE INDEX idx_payments_status     ON payments(status);
CREATE INDEX idx_webhooks_payment_id ON payment_webhooks(payment_id);
CREATE INDEX idx_webhooks_processed  ON payment_webhooks(processed);
