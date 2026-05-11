package com.vivaeventos.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentWebhook;
import com.vivaeventos.paymentservice.dto.WompiWebhookRequest;
import com.vivaeventos.paymentservice.kafka.KafkaPublisher;
import com.vivaeventos.paymentservice.repository.PaymentRepository;
import com.vivaeventos.paymentservice.repository.PaymentWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookRepository webhookRepository;
    private final KafkaPublisher kafkaPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processWebhook(WompiWebhookRequest request) {
        String gatewayPaymentId = request.data().transaction().id();
        String eventType = request.event();

        // Idempotencia: ignorar si este evento ya fue procesado para este pago
        if (webhookRepository.existsByPayment_GatewayPaymentIdAndEventType(gatewayPaymentId, eventType)) {
            log.warn("Webhook duplicado ignorado: gatewayPaymentId={} event={}", gatewayPaymentId, eventType);
            return;
        }

        Payment payment = paymentRepository.findByGatewayPaymentId(gatewayPaymentId).orElse(null);

        PaymentWebhook webhook = PaymentWebhook.builder()
            .payment(payment)
            .eventType(eventType)
            .rawPayload(serialize(request))
            .processed(false)
            .receivedAt(LocalDateTime.now())
            .build();

        if (payment == null) {
            log.warn("Pago no encontrado para gatewayPaymentId={}", gatewayPaymentId);
            webhookRepository.save(webhook);
            return;
        }

        String gatewayStatus = request.data().transaction().status();

        if ("APPROVED".equalsIgnoreCase(gatewayStatus)) {
            payment.setStatus("CONFIRMED");
            payment.setConfirmedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            webhook.setProcessed(true);
            webhookRepository.save(webhook);
            kafkaPublisher.publishPagoConfirmado(payment);

        } else if ("DECLINED".equalsIgnoreCase(gatewayStatus) || "VOIDED".equalsIgnoreCase(gatewayStatus)) {
            payment.setStatus("FAILED");
            payment.setFailedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            webhook.setProcessed(true);
            webhookRepository.save(webhook);
            kafkaPublisher.publishPagoFallido(payment);

        } else {
            log.info("Estado de pasarela no manejado: status={} paymentId={}", gatewayStatus, payment.getId());
            webhookRepository.save(webhook);
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
