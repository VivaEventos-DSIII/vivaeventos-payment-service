package com.vivaeventos.paymentservice.event;

import java.util.UUID;

public record PagoConfirmadoEvent(
        UUID orderId,
        UUID ticketTypeId,
        String customerEmail,
        Long amount
) {}
