package com.vivaeventos.paymentservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefundResponseDto(
        UUID refundId,
        String status,
        LocalDateTime requestedAt
) {}
