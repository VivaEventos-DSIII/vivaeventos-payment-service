package com.vivaeventos.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequestDto(
        UUID orderId,
        UUID eventId,
        UUID customerId,
        String userEmail,
        String userName,
        BigDecimal totalAmount,
        String reason
) {}
