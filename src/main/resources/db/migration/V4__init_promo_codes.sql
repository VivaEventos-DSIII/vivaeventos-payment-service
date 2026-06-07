CREATE TABLE promo_codes (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(50)   NOT NULL UNIQUE,
    discount_type     VARCHAR(20)   NOT NULL,
    discount_value    DECIMAL(10,2) NOT NULL,
    active            BOOLEAN       NOT NULL DEFAULT true,
    expiration_date   TIMESTAMP     NOT NULL,
    usage_limit       INTEGER,
    used_count        INTEGER       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_promo_code          ON promo_codes(code);
CREATE INDEX idx_promo_active        ON promo_codes(active);
CREATE INDEX idx_promo_expiration    ON promo_codes(expiration_date);

-- Ejemplo de datos (opcional, comentado)
-- INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit)
-- VALUES
--     ('SUMMER20', 'PERCENTAGE', 20.00, true, '2026-12-31 23:59:59', 1000),
--     ('NEW100', 'FIXED', 100000.00, true, '2026-08-31 23:59:59', 500),
--     ('EXPIRED10', 'PERCENTAGE', 10.00, true, '2025-01-01 23:59:59', 100);

