package com.vivaeventos.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DevolucionSolicitadaEvent(
        UUID orderId,
        UUID eventId,
        UUID customerId,
        String userEmail,
        String userName,
        BigDecimal totalAmount,
        String reason,
        LocalDateTime requestedAt
) {}
