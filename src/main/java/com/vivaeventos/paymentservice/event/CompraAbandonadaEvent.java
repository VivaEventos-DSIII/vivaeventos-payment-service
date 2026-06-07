package com.vivaeventos.paymentservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompraAbandonadaEvent(
        UUID orderId,
        UUID paymentId,
        String customerEmail,
        Long amountInCents,
        LocalDateTime detectedAt
) {}
