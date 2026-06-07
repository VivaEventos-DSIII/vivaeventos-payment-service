package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.config.WompiConfig;
import com.vivaeventos.paymentservice.event.CompraAbandonadaEvent;
import com.vivaeventos.paymentservice.model.Abandono;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentStatus;
import com.vivaeventos.paymentservice.repository.AbandonoRepository;
import com.vivaeventos.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbandonoService {

    private final PaymentRepository paymentRepository;
    private final AbandonoRepository abandonoRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WompiConfig wompiConfig;

    @Scheduled(fixedDelayString = "${payment.gateway.reconciliation-interval-ms:300000}")
    public void detectarAbandonos() {
        LocalDateTime umbral = LocalDateTime.now().minusMinutes(wompiConfig.getAbandonmentThresholdMinutes());
        List<Payment> pagos = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDIENTE, umbral);

        if (pagos.isEmpty()) {
            return;
        }

        log.info("Detección de abandonos: {} pagos PENDIENTE superan el umbral de {} min",
                pagos.size(), wompiConfig.getAbandonmentThresholdMinutes());

        for (Payment pago : pagos) {
            try {
                if (!abandonoRepository.existsByPayment_Id(pago.getId())) {
                    registrarAbandono(pago);
                }
            } catch (Exception e) {
                log.error("Error al registrar abandono para orderId={}: {}", pago.getOrderId(), e.getMessage());
            }
        }
    }

    private void registrarAbandono(Payment pago) {
        int minutosPendiente = (int) ChronoUnit.MINUTES.between(pago.getCreatedAt(), LocalDateTime.now());

        Abandono abandono = Abandono.builder()
                .payment(pago)
                .orderId(pago.getOrderId())
                .customerEmail(pago.getCustomerEmail())
                .amountInCents(pago.getAmountInCents())
                .minutesPending(minutosPendiente)
                .detectedAt(LocalDateTime.now())
                .build();

        abandonoRepository.save(abandono);

        CompraAbandonadaEvent evento = new CompraAbandonadaEvent(
                pago.getOrderId(),
                pago.getId(),
                pago.getCustomerEmail(),
                pago.getAmountInCents(),
                abandono.getDetectedAt()
        );
        kafkaTemplate.send("compra-abandonada", pago.getOrderId().toString(), evento);

        log.info("Abandono registrado: orderId={} customerEmail={} minutesPendiente={}",
                pago.getOrderId(), pago.getCustomerEmail(), minutosPendiente);
    }
}
