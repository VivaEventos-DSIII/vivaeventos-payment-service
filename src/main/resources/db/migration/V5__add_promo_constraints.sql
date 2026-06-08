-- Elimina el índice regular redundante: la restricción UNIQUE en 'code' ya crea su propio índice
DROP INDEX IF EXISTS idx_promo_code;

-- Restringe discount_type a los únicos valores válidos del enum
ALTER TABLE promo_codes
    ADD CONSTRAINT chk_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED'));

-- Restringe discount_value a valores estrictamente positivos
ALTER TABLE promo_codes
    ADD CONSTRAINT chk_discount_value_positive
        CHECK (discount_value > 0);

-- Restringe used_count a valores no negativos
ALTER TABLE promo_codes
    ADD CONSTRAINT chk_used_count_non_negative
        CHECK (used_count >= 0);
