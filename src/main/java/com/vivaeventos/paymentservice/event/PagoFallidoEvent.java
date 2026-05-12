package com.vivaeventos.paymentservice.event;

import java.util.UUID;

public record PagoFallidoEvent(
        UUID orderId,
        String motivo
) {}
