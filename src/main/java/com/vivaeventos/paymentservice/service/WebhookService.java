package com.vivaeventos.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivaeventos.paymentservice.dto.WompiWebhookRequest;
import com.vivaeventos.paymentservice.kafka.PaymentEventPublisher;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentStatus;
import com.vivaeventos.paymentservice.model.PaymentWebhook;
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
    private final PaymentEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processWebhook(WompiWebhookRequest request) {
        String wompiTransactionId = request.data().transaction().id();
        String reference = request.data().transaction().reference();
        String eventType = request.event();

        if (webhookRepository.existsByPayment_WompiTransactionIdAndEventType(wompiTransactionId, eventType)) {
            log.warn("Webhook duplicado ignorado: wompiId={} event={}", wompiTransactionId, eventType);
            return;
        }

        Payment payment = paymentRepository.findByReference(reference).orElse(null);

        PaymentWebhook webhook = PaymentWebhook.builder()
                .payment(payment)
                .eventType(eventType)
                .rawPayload(serialize(request))
                .processed(false)
                .build();

        if (payment == null) {
            log.warn("Pago no encontrado para reference={}", reference);
            webhookRepository.save(webhook);
            return;
        }

        String gatewayStatus = request.data().transaction().status();

        switch (gatewayStatus.toUpperCase()) {
            case "APPROVED" -> {
                payment.setStatus(PaymentStatus.APROBADO);
                payment.setWompiTransactionId(wompiTransactionId);
                payment.setPaymentMethodType(request.data().transaction().paymentMethodType());
                paymentRepository.save(payment);
                webhook.setProcessed(true);
                webhookRepository.save(webhook);
                eventPublisher.publishPagoConfirmado(payment);
                log.info("Pago APROBADO: reference={}", reference);
            }
            case "DECLINED" -> {
                payment.setStatus(PaymentStatus.DECLINADO);
                paymentRepository.save(payment);
                webhook.setProcessed(true);
                webhookRepository.save(webhook);
                eventPublisher.publishPagoFallido(payment, "Pago declinado por la pasarela");
                log.info("Pago DECLINADO: reference={}", reference);
            }
            case "VOIDED", "ERROR" -> {
                payment.setStatus(PaymentStatus.FALLIDO);
                paymentRepository.save(payment);
                webhook.setProcessed(true);
                webhookRepository.save(webhook);
                eventPublisher.publishPagoFallido(payment, "Transacción anulada o con error: " + gatewayStatus);
                log.info("Pago FALLIDO ({}): reference={}", gatewayStatus, reference);
            }
            default -> {
                log.info("Estado de pasarela no manejado: status={} reference={}", gatewayStatus, reference);
                webhookRepository.save(webhook);
            }
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
