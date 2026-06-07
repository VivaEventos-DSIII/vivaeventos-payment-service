package com.vivaeventos.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ValidatePromocodeResponseDto(
        UUID promocodeId,
        String code,
        String discountType,
        BigDecimal discountValue,
        Boolean active,
        LocalDateTime expirationDate,
        Integer usageLimit,
        Integer usedCount,
        Boolean valid,
        String message
) {}

