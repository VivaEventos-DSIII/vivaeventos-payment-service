package com.vivaeventos.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivaeventos.paymentservice.config.WompiConfig;
import com.vivaeventos.paymentservice.dto.WompiWebhookRequest;
import com.vivaeventos.paymentservice.exception.SignatureVerificationException;
import com.vivaeventos.paymentservice.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class WompiWebhookController {

    private final WebhookService webhookService;
    private final WompiConfig wompiConfig;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String rawBody) {
        WompiWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, WompiWebhookRequest.class);
        } catch (Exception e) {
            log.warn("Webhook con payload inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        if (!isSignatureValid(request)) {
            log.warn("Webhook rechazado: firma inválida para event={}", request.event());
            throw new SignatureVerificationException("Firma del webhook no coincide");
        }

        log.info("Webhook recibido de Wompi: event={} reference={}",
                request.event(), request.data().transaction().reference());
        webhookService.processWebhook(request);
        return ResponseEntity.ok().build();
    }

    private boolean isSignatureValid(WompiWebhookRequest request) {
        var sig = request.signature();
        if (sig == null || sig.checksum() == null || sig.properties() == null || sig.timestamp() == null) {
            log.warn("Webhook sin campo signature — rechazado");
            return false;
        }

        var tx = request.data().transaction();
        StringBuilder sb = new StringBuilder();
        for (String property : sig.properties()) {
            String value = switch (property) {
                case "transaction.id"             -> tx.id() != null ? tx.id() : "";
                case "transaction.status"         -> tx.status() != null ? tx.status() : "";
                case "transaction.amount_in_cents" -> tx.amountInCents() != null ? String.valueOf(tx.amountInCents()) : "";
                default -> {
                    log.debug("Propiedad de firma desconocida: {}", property);
                    yield "";
                }
            };
            sb.append(value);
        }
        sb.append(sig.timestamp());
        sb.append(wompiConfig.getWebhookSecret());

        String computed = sha256Hex(sb.toString());
        boolean valid = computed.equalsIgnoreCase(sig.checksum());
        if (!valid) {
            log.warn("SHA-256 esperado={} recibido={}", computed, sig.checksum());
        }
        return valid;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
