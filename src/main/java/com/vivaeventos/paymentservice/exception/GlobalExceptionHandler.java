package com.vivaeventos.paymentservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SignatureVerificationException.class)
    public ProblemDetail handleSignatureVerification(SignatureVerificationException ex) {
        log.warn("Firma inválida en webhook: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Webhook Signature Invalid");
        pd.setType(URI.create("urn:vivaeventos:error:invalid-signature"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ProblemDetail handleWompiError(WebClientResponseException ex) {
        log.error("Error HTTP desde Wompi: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "Error al comunicarse con la pasarela de pagos");
        pd.setTitle("Payment Gateway Error");
        pd.setType(URI.create("urn:vivaeventos:error:gateway-error"));
        pd.setProperty("gatewayStatus", ex.getStatusCode().value());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.error("Estado ilegal: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setTitle("Internal Error");
        pd.setType(URI.create("urn:vivaeventos:error:internal"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Error no controlado: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("urn:vivaeventos:error:internal"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
