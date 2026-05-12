package com.vivaeventos.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record WompiTransactionRequest(
        @JsonProperty("amount_in_cents") Long amountInCents,
        String currency,
        @JsonProperty("customer_email") String customerEmail,
        @JsonProperty("payment_method") PaymentMethod paymentMethod,
        String reference,
        @JsonProperty("acceptance_token") String acceptanceToken,
        @JsonProperty("customer_data") CustomerData customerData
) {
    @Builder
    public record PaymentMethod(
            String type,
            String token,
            Integer installments
    ) {}

    @Builder
    public record CustomerData(
            @JsonProperty("full_name") String fullName,
            @JsonProperty("phone_number") String phoneNumber
    ) {}
}
