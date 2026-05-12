package com.vivaeventos.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WompiTransactionResponse(Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String id,
            String status,
            String reference,
            @JsonProperty("amount_in_cents") Long amountInCents,
            String currency,
            @JsonProperty("payment_method_type") String paymentMethodType
    ) {}
}
