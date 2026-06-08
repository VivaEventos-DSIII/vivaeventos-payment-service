package com.vivaeventos.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequestDto(
        UUID orderId,
        UUID eventId,
        String userName,
        BigDecimal totalAmount,
        String reason
) {}
