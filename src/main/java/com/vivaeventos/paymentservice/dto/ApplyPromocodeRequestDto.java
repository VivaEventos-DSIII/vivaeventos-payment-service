package com.vivaeventos.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyPromocodeRequestDto(
        @NotBlank(message = "El código promocional no puede estar vacío")
        String code,

        @NotNull(message = "El orderId no puede ser nulo")
        UUID orderId,

        @NotNull(message = "El monto original no puede ser nulo")
        @DecimalMin(value = "0.01", message = "El monto original debe ser mayor a cero")
        BigDecimal originalAmount
) {}
