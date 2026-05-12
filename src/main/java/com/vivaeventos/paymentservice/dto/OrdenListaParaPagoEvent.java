package com.vivaeventos.paymentservice.dto;

import java.util.UUID;

public record OrdenListaParaPagoEvent(
        UUID orderId,
        UUID ticketTypeId,
        String customerEmail,
        String customerName,
        Long amountInCents,
        String cardToken
) {}
