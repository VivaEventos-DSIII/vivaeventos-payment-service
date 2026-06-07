package com.vivaeventos.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyPromocodeRequestDto(
        String code,
        UUID orderId,
        BigDecimal originalAmount
) {}

