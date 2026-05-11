package com.vivaeventos.paymentservice.event;

import java.util.UUID;

public record PagoConfirmadoEvent(
    UUID paymentId,
    UUID orderId,
    String gatewayPaymentId,
    Double amount,
    String currency
) {}
