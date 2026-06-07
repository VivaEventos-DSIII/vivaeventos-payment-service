package com.vivaeventos.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyPromocodeResponseDto(
        UUID promocodeId,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal discountApplied,
        BigDecimal newTotal,
        Long newTotalInCents,
        String message
) {}

