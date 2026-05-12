package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.config.WompiConfig;
import com.vivaeventos.paymentservice.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class WompiClient {

    private final WebClient.Builder webClientBuilder;
    private final WompiConfig wompiConfig;

    private WebClient buildClient() {
        return webClientBuilder
                .baseUrl(wompiConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "acceptanceTokenFallback")
    public String getAcceptanceToken() {
        log.debug("Obteniendo acceptance token de Wompi para publicKey={}", wompiConfig.getPublicKey());
        WompiMerchantResponse response = buildClient().get()
                .uri("/merchants/{publicKey}", wompiConfig.getPublicKey())
                .retrieve()
                .bodyToMono(WompiMerchantResponse.class)
                .timeout(Duration.ofSeconds(wompiConfig.getTimeoutSeconds()))
                .block();

        if (response == null || response.data() == null || response.data().presignedAcceptance() == null) {
            throw new IllegalStateException("Respuesta de Wompi merchants inválida");
        }
        return response.data().presignedAcceptance().acceptanceToken();
    }

    @SuppressWarnings("unused")
    private String acceptanceTokenFallback(Throwable t) {
        log.error("Circuit breaker activado al obtener acceptance token: {}", t.getMessage());
        throw new RuntimeException("Pasarela de pagos no disponible. Intente más tarde.");
    }

    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "createTransactionFallback")
    public WompiTransactionResponse createTransaction(WompiTransactionRequest request) {
        log.info("Creando transacción en Wompi: reference={}, amount={}", request.reference(), request.amountInCents());
        try {
            return buildClient().post()
                    .uri("/transactions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + wompiConfig.getPrivateKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(WompiTransactionResponse.class)
                    .timeout(Duration.ofSeconds(wompiConfig.getTimeoutSeconds()))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error HTTP de Wompi al crear transacción: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    @SuppressWarnings("unused")
    private WompiTransactionResponse createTransactionFallback(WompiTransactionRequest request, Throwable t) {
        log.error("Circuit breaker activado al crear transacción: reference={} error={}", request.reference(), t.getMessage());
        throw new RuntimeException("Pasarela de pagos no disponible. Intente más tarde.");
    }

    public WompiTransactionResponse getTransaction(String transactionId) {
        log.debug("Consultando transacción en Wompi: id={}", transactionId);
        return buildClient().get()
                .uri("/transactions/{id}", transactionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wompiConfig.getPrivateKey())
                .retrieve()
                .bodyToMono(WompiTransactionResponse.class)
                .timeout(Duration.ofSeconds(wompiConfig.getTimeoutSeconds()))
                .block();
    }
}
