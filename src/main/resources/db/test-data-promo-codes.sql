-- Comandos SQL para insertar códigos promocionales de prueba
-- Ejecutar SOLO para ambiente local/desarrollo

-- Limpiar códigos existentes (opcional)
-- DELETE FROM promo_codes;

-- Descuento Porcentual - 20% válido hasta fin de 2026
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('SUMMER20', 'PERCENTAGE', 20.00, true, '2026-12-31 23:59:59', 1000, 0);

-- Descuento Porcentual - 10% sin límite de usos
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('WELCOME10', 'PERCENTAGE', 10.00, true, '2026-12-31 23:59:59', NULL, 0);

-- Descuento Fijo - 100K COP
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('FIXED100K', 'FIXED', 100000.00, true, '2026-08-31 23:59:59', 500, 0);

-- Descuento Porcentual - 50% (Black Friday)
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('BLACKFRIDAY50', 'PERCENTAGE', 50.00, true, '2026-11-30 23:59:59', 100, 0);

-- EXPIRADO - Código expirado para pruebas
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('EXPIRED2025', 'PERCENTAGE', 15.00, true, '2025-06-06 23:59:59', 1000, 0);

-- INACTIVO - Código desactivado
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('INACTIVE', 'PERCENTAGE', 25.00, false, '2026-12-31 23:59:59', 1000, 0);

-- LÍMITE ALCANZADO - Ya se utilizó el límite
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('LIMITREACHED', 'PERCENTAGE', 30.00, true, '2026-12-31 23:59:59', 5, 5);

-- Descuento Fijo Pequeño
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('SMALL5K', 'FIXED', 5000.00, true, '2026-12-31 23:59:59', 2000, 0);

-- Descuento Porcentual Muy Alto
INSERT INTO promo_codes (code, discount_type, discount_value, active, expiration_date, usage_limit, used_count)
VALUES ('MEGA100', 'PERCENTAGE', 100.00, true, '2026-12-31 23:59:59', 50, 0);

-- Consultas de prueba útiles:
-- SELECT * FROM promo_codes WHERE active = true ORDER BY created_at DESC;
-- SELECT count(*) as "Total Códigos", count(usage_limit) as "Con Límite" FROM promo_codes;
-- SELECT code, used_count, usage_limit, (used_count::float/usage_limit)*100 as "% Usado" FROM promo_codes WHERE usage_limit IS NOT NULL;

