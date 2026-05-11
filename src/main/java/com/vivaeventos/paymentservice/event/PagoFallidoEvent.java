package com.vivaeventos.paymentservice.event;

import java.util.UUID;

public record PagoFallidoEvent(
    UUID paymentId,
    UUID orderId,
    String gatewayPaymentId,
    String reason
) {}
