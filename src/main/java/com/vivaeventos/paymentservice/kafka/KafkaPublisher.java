package com.vivaeventos.paymentservice.kafka;

import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.event.PagoConfirmadoEvent;
import com.vivaeventos.paymentservice.event.PagoFallidoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-confirmed}")
    private String confirmedTopic;

    @Value("${kafka.topics.payment-failed}")
    private String failedTopic;

    public void publishPagoConfirmado(Payment payment) {
        var event = new PagoConfirmadoEvent(
            payment.getId(),
            payment.getOrderId(),
            payment.getGatewayPaymentId(),
            payment.getAmount(),
            payment.getCurrency()
        );
        kafkaTemplate.send(confirmedTopic, payment.getId().toString(), event);
        log.info("PagoConfirmado publicado: paymentId={}", payment.getId());
    }

    public void publishPagoFallido(Payment payment) {
        var event = new PagoFallidoEvent(
            payment.getId(),
            payment.getOrderId(),
            payment.getGatewayPaymentId(),
            "Pago rechazado por la pasarela"
        );
        kafkaTemplate.send(failedTopic, payment.getId().toString(), event);
        log.info("PagoFallido publicado: paymentId={}", payment.getId());
    }
}
