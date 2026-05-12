package com.vivaeventos.paymentservice.kafka;

import com.vivaeventos.paymentservice.event.PagoConfirmadoEvent;
import com.vivaeventos.paymentservice.event.PagoFallidoEvent;
import com.vivaeventos.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private static final String TOPIC_CONFIRMADO = "pago-confirmado";
    private static final String TOPIC_FALLIDO = "pago-fallido";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPagoConfirmado(Payment payment) {
        var event = new PagoConfirmadoEvent(
                payment.getOrderId(),
                payment.getTicketTypeId(),
                payment.getCustomerEmail(),
                payment.getAmountInCents()
        );
        kafkaTemplate.send(TOPIC_CONFIRMADO, payment.getOrderId().toString(), event);
        log.info("PagoConfirmado publicado: orderId={}", payment.getOrderId());
    }

    public void publishPagoFallido(Payment payment, String motivo) {
        var event = new PagoFallidoEvent(payment.getOrderId(), motivo);
        kafkaTemplate.send(TOPIC_FALLIDO, payment.getOrderId().toString(), event);
        log.info("PagoFallido publicado: orderId={} motivo={}", payment.getOrderId(), motivo);
    }
}
