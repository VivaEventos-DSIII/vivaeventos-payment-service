package com.vivaeventos.paymentservice.kafka;

import com.vivaeventos.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @deprecated Use {@link PaymentEventPublisher} instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Deprecated(forRemoval = true)
public class KafkaPublisher {

    private final PaymentEventPublisher delegate;

    public void publishPagoConfirmado(Payment payment) {
        delegate.publishPagoConfirmado(payment);
    }

    public void publishPagoFallido(Payment payment) {
        delegate.publishPagoFallido(payment, "Pago rechazado por la pasarela");
    }
}
