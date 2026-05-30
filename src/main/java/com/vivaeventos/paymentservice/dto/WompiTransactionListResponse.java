package com.vivaeventos.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WompiTransactionListResponse(List<WompiTransactionResponse.Data> data) {}
