package com.vivaeventos.paymentservice.controller;

import com.vivaeventos.paymentservice.dto.WompiWebhookRequest;
import com.vivaeventos.paymentservice.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class WompiWebhookController {

    private final WebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody WompiWebhookRequest request) {
        log.info("Webhook recibido de Wompi: event={}", request.event());
        webhookService.processWebhook(request);
        return ResponseEntity.ok().build();
    }
}
