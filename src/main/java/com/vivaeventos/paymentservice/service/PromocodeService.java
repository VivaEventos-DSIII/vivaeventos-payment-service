package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.dto.ApplyPromocodeRequestDto;
import com.vivaeventos.paymentservice.dto.ApplyPromocodeResponseDto;
import com.vivaeventos.paymentservice.dto.ValidatePromocodeResponseDto;
import com.vivaeventos.paymentservice.exception.PromocodeException;
import com.vivaeventos.paymentservice.model.PromoCode;
import com.vivaeventos.paymentservice.repository.PromocodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromocodeService {

    private final PromocodeRepository promocodeRepository;

    /**
     * Valida un código promocional sin aplicar descuento
     * @param code el código a validar
     * @return información detallada del código promocional
     * @throws PromocodeException si el código no existe o es inválido
     */
    public ValidatePromocodeResponseDto validarCodigo(String code) {
        PromoCode promoCode = buscarCodigoOThrow(code);
        
        String validationMessage = validarEstadoDelCodigo(promoCode);
        boolean isValid = validationMessage == null;

        return new ValidatePromocodeResponseDto(
                promoCode.getId(),
                promoCode.getCode(),
                promoCode.getDiscountType().toString(),
                promoCode.getDiscountValue(),
                promoCode.getActive(),
                promoCode.getExpirationDate(),
                promoCode.getUsageLimit(),
                promoCode.getUsedCount(),
                isValid,
                isValid ? "Código válido y activo" : validationMessage
        );
    }

    /**
     * Aplica un código promocional a un monto y calcula el descuento
     * @param request DTO con código, orderId y monto original
     * @return DTO con detalles del descuento aplicado y nuevo total
     * @throws PromocodeException si el código no es válido o no se puede aplicar
     */
    @Transactional
    public ApplyPromocodeResponseDto aplicarCodigo(ApplyPromocodeRequestDto request) {
        BigDecimal originalAmount = request.originalAmount();
        
        // Validaciones de entrada
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PromocodeException(
                    "El monto original debe ser mayor a cero",
                    "INVALID_AMOUNT"
            );
        }

        // Buscar el código con bloqueo pesimista para evitar race conditions en el límite de usos
        PromoCode promoCode = buscarCodigoConBloqueoOThrow(request.code());

        // Validar estado del código
        String estadoInvalido = validarEstadoDelCodigo(promoCode);
        if (estadoInvalido != null) {
            throw new PromocodeException(estadoInvalido, "INVALID_PROMOCODE");
        }

        // Calcular descuento basado en tipo
        BigDecimal discountApplied = calcularDescuento(promoCode, originalAmount);

        // Calcular nuevo total (no permitir totales negativos)
        BigDecimal newTotal = originalAmount.subtract(discountApplied);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            newTotal = BigDecimal.ZERO;
        }

        // Incrementar contador de usos
        promoCode.setUsedCount(promoCode.getUsedCount() + 1);
        promocodeRepository.save(promoCode);
        log.info("Código promocional aplicado: code={} orderId={} descuento={} COP", 
                promoCode.getCode(), request.orderId(), discountApplied);

        // Convertir a centavos para la respuesta
        Long newTotalInCents = newTotal.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        return new ApplyPromocodeResponseDto(
                promoCode.getId(),
                promoCode.getCode(),
                promoCode.getDiscountType().toString(),
                promoCode.getDiscountValue(),
                discountApplied,
                newTotal,
                newTotalInCents,
                "Descuento aplicado exitosamente"
        );
    }

    /**
     * Calcula el descuento según el tipo (porcentaje o valor fijo)
     */
    private BigDecimal calcularDescuento(PromoCode promoCode, BigDecimal originalAmount) {
        if (promoCode.getDiscountType() == PromoCode.DiscountType.PERCENTAGE) {
            // Descuento porcentual: (originalAmount * discountValue) / 100
            return originalAmount.multiply(promoCode.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // Descuento fijo: no puede ser mayor al monto original
            return promoCode.getDiscountValue().min(originalAmount);
        }
    }

    /**
     * Realiza todas las validaciones del código
     * @return null si es válido, mensaje de error si no
     */
    private String validarEstadoDelCodigo(PromoCode promoCode) {
        // 1. Validar que esté activo
        if (!promoCode.getActive()) {
            return "El código promocional está inactivo";
        }

        // 2. Validar que no esté expirado
        if (LocalDateTime.now().isAfter(promoCode.getExpirationDate())) {
            return "El código promocional ha expirado";
        }

        // 3. Validar límite de usos (si está definido)
        if (promoCode.getUsageLimit() != null &&
            promoCode.getUsedCount() >= promoCode.getUsageLimit()) {
            return "El código promocional ha alcanzado su límite de usos";
        }

        // 4. Validar que el porcentaje de descuento esté en rango válido
        if (promoCode.getDiscountType() == PromoCode.DiscountType.PERCENTAGE) {
            BigDecimal value = promoCode.getDiscountValue();
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                return "El código tiene un porcentaje de descuento inválido (debe estar entre 1 y 100)";
            }
        }

        return null; // Todo válido
    }

    private PromoCode buscarCodigoOThrow(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new PromocodeException(
                    "El código promocional no puede estar vacío",
                    "EMPTY_CODE"
            );
        }

        return promocodeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> {
                    log.warn("Código promocional no encontrado: {}", code);
                    return new PromocodeException(
                            "El código promocional no existe",
                            "PROMOCODE_NOT_FOUND"
                    );
                });
    }

    private PromoCode buscarCodigoConBloqueoOThrow(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new PromocodeException(
                    "El código promocional no puede estar vacío",
                    "EMPTY_CODE"
            );
        }

        return promocodeRepository.findByCodeIgnoreCaseForUpdate(code.trim())
                .orElseThrow(() -> {
                    log.warn("Código promocional no encontrado: {}", code);
                    return new PromocodeException(
                            "El código promocional no existe",
                            "PROMOCODE_NOT_FOUND"
                    );
                });
    }
}

