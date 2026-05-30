package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.config.WompiConfig;
import com.vivaeventos.paymentservice.dto.WompiTransactionResponse;
import com.vivaeventos.paymentservice.kafka.PaymentEventPublisher;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentStatus;
import com.vivaeventos.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final PaymentRepository paymentRepository;
    private final WompiClient wompiClient;
    private final PaymentEventPublisher eventPublisher;
    private final WompiConfig wompiConfig;

    @Scheduled(fixedDelayString = "${payment.gateway.reconciliation-interval-ms:300000}")
    public void reconciliarPagosPendientes() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(wompiConfig.getPendingThresholdMinutes());
        List<Payment> pagos = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDIENTE, threshold);

        if (pagos.isEmpty()) {
            return;
        }

        log.info("Reconciliación: {} pagos PENDIENTE a verificar con Wompi", pagos.size());

        for (Payment pago : pagos) {
            try {
                reconciliarPago(pago);
            } catch (Exception e) {
                log.error("Reconciliación fallida para reference={}: {}", pago.getReference(), e.getMessage());
            }
        }
    }

    private void reconciliarPago(Payment pago) {
        WompiTransactionResponse response;
        if (pago.getWompiTransactionId() != null) {
            response = wompiClient.getTransaction(pago.getWompiTransactionId());
        } else {
            response = wompiClient.getTransactionByReference(pago.getReference());
        }

        if (response == null || response.data() == null) {
            log.warn("Reconciliación: Wompi no devolvió información para reference={}", pago.getReference());
            return;
        }

        String status = response.data().status();
        log.info("Reconciliación: reference={} statusEnWompi={}", pago.getReference(), status);

        switch (status.toUpperCase()) {
            case "APPROVED" -> {
                pago.setStatus(PaymentStatus.APROBADO);
                pago.setWompiTransactionId(response.data().id());
                pago.setPaymentMethodType(response.data().paymentMethodType());
                paymentRepository.save(pago);
                try {
                    eventPublisher.publishPagoConfirmado(pago);
                } catch (Exception e) {
                    log.error("No se pudo publicar evento pago-confirmado para reference={}: {}", pago.getReference(), e.getMessage());
                }
                log.info("Reconciliación: pago APROBADO: reference={}", pago.getReference());
            }
            case "DECLINED" -> {
                pago.setStatus(PaymentStatus.DECLINADO);
                paymentRepository.save(pago);
                try {
                    eventPublisher.publishPagoFallido(pago, "Pago declinado por la pasarela");
                } catch (Exception e) {
                    log.error("No se pudo publicar evento pago-fallido para reference={}: {}", pago.getReference(), e.getMessage());
                }
                log.info("Reconciliación: pago DECLINADO: reference={}", pago.getReference());
            }
            case "VOIDED", "ERROR" -> {
                pago.setStatus(PaymentStatus.FALLIDO);
                paymentRepository.save(pago);
                try {
                    eventPublisher.publishPagoFallido(pago, "Transacción anulada o con error: " + status);
                } catch (Exception e) {
                    log.error("No se pudo publicar evento pago-fallido para reference={}: {}", pago.getReference(), e.getMessage());
                }
                log.info("Reconciliación: pago FALLIDO ({}): reference={}", status, pago.getReference());
            }
            case "PENDING" -> {
                if (pago.getCreatedAt() != null &&
                        pago.getCreatedAt().isBefore(LocalDateTime.now().minusHours(wompiConfig.getMaxPendingAgeHours()))) {
                    pago.setStatus(PaymentStatus.FALLIDO);
                    paymentRepository.save(pago);
                    try {
                        eventPublisher.publishPagoFallido(pago, "Tiempo máximo de espera de confirmación superado");
                    } catch (Exception e) {
                        log.error("No se pudo publicar evento pago-fallido para reference={}: {}", pago.getReference(), e.getMessage());
                    }
                    log.warn("Reconciliación: pago expirado por tiempo máximo ({}h): reference={}", wompiConfig.getMaxPendingAgeHours(), pago.getReference());
                } else {
                    log.debug("Reconciliación: pago sigue PENDIENTE en Wompi: reference={}", pago.getReference());
                }
            }
            default -> log.warn("Reconciliación: estado desconocido de Wompi: status={} reference={}", status, pago.getReference());
        }
    }
}
