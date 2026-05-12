package com.vivaeventos.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WompiMerchantResponse(Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("presigned_acceptance") PresignedAcceptance presignedAcceptance
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PresignedAcceptance(
            @JsonProperty("acceptance_token") String acceptanceToken
    ) {}
}
