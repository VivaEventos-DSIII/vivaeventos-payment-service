package com.vivaeventos.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WompiWebhookRequest(
    String event,
    Data data,
    @JsonProperty("sent_at") String sentAt
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(Transaction transaction) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(
        String id,
        String status,
        String reference,
        @JsonProperty("amount_in_cents") Long amountInCents,
        String currency
    ) {}
}
