-- V1__init_payments.sql
-- Schema inicial del payment-service
-- Historias: US-05, US-19, RQ-07 (estados asíncronos), RQ-14 (idempotencia)

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID           NOT NULL,
    gateway_payment_id  VARCHAR(255),          -- ID retornado por la pasarela
    amount              NUMERIC(12, 2) NOT NULL,
    currency            VARCHAR(10)    NOT NULL DEFAULT 'COP',
    status              VARCHAR(50)    NOT NULL DEFAULT 'INITIATED',
    -- INITIATED | PENDING | PROCESSING | CONFIRMED | FAILED | REFUNDED
    gateway_response    JSONB,                 -- Respuesta raw de la pasarela (trazabilidad RQ-09)
    idempotency_key     VARCHAR(255)   UNIQUE, -- RQ-14: evitar doble cobro
    initiated_at        TIMESTAMP      NOT NULL DEFAULT now(),
    confirmed_at        TIMESTAMP,
    failed_at           TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT now()
);

-- Registro de webhooks recibidos de la pasarela (US-19, RQ-07)
CREATE TABLE payment_webhooks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID           REFERENCES payments(id),
    event_type      VARCHAR(100)   NOT NULL,   -- e.g. payment.confirmed, payment.failed
    raw_payload     JSONB          NOT NULL,
    processed       BOOLEAN        NOT NULL DEFAULT FALSE,
    received_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id          ON payments(order_id);
CREATE INDEX idx_payments_gateway_id        ON payments(gateway_payment_id);
CREATE INDEX idx_payments_status            ON payments(status);
CREATE INDEX idx_webhooks_payment_id        ON payment_webhooks(payment_id);
CREATE INDEX idx_webhooks_processed         ON payment_webhooks(processed);
