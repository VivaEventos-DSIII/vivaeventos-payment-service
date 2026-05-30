package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.dto.OrdenListaParaPagoEvent;
import com.vivaeventos.paymentservice.dto.WompiTransactionRequest;
import com.vivaeventos.paymentservice.dto.WompiTransactionResponse;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentStatus;
import com.vivaeventos.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WompiClient wompiClient;

    @Transactional
    public void initiatePago(OrdenListaParaPagoEvent event) {
        String reference = event.orderId().toString();

        if (paymentRepository.existsByReference(reference)) {
            log.warn("Orden ya fue procesada (idempotencia): orderId={}", event.orderId());
            return;
        }

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .ticketTypeId(event.ticketTypeId())
                .reference(reference)
                .amountInCents(event.amountInCents())
                .currency("COP")
                .status(PaymentStatus.PENDIENTE)
                .customerEmail(event.customerEmail())
                .build();
        paymentRepository.save(payment);
        log.info("Pago creado en PENDIENTE: reference={}", reference);

        String acceptanceToken;
        try {
            acceptanceToken = wompiClient.getAcceptanceToken();
        } catch (Exception e) {
            // Sin token no se envió nada a Wompi → es seguro marcar FALLIDO
            log.error("Error al obtener acceptance token para orderId={}: {}", event.orderId(), e.getMessage());
            payment.setStatus(PaymentStatus.FALLIDO);
            paymentRepository.save(payment);
            return;
        }

        WompiTransactionRequest transactionRequest = WompiTransactionRequest.builder()
                .amountInCents(event.amountInCents())
                .currency("COP")
                .customerEmail(event.customerEmail())
                .reference(reference)
                .acceptanceToken(acceptanceToken)
                .paymentMethod(WompiTransactionRequest.PaymentMethod.builder()
                        .type("CARD")
                        .token(event.cardToken())
                        .installments(1)
                        .build())
                .customerData(WompiTransactionRequest.CustomerData.builder()
                        .fullName(event.customerName())
                        .phoneNumber("3000000000")
                        .build())
                .build();

        try {
            WompiTransactionResponse response = wompiClient.createTransaction(transactionRequest);
            if (response != null && response.data() != null) {
                payment.setWompiTransactionId(response.data().id());
                payment.setPaymentMethodType(response.data().paymentMethodType());
                paymentRepository.save(payment);
                log.info("Transacción Wompi creada: wompiId={} status={}", response.data().id(), response.data().status());
            }
        } catch (WebClientResponseException e) {
            // 4xx: Wompi rechazó la solicitud explícitamente → FALLIDO definitivo
            if (e.getStatusCode().is4xxClientError()) {
                log.error("Wompi rechazó la transacción ({}): orderId={}", e.getStatusCode(), event.orderId());
                payment.setStatus(PaymentStatus.FALLIDO);
                paymentRepository.save(payment);
            } else {
                // 5xx: error transitorio en Wompi → dejamos PENDIENTE para reconciliación
                log.warn("Error transitorio de Wompi ({}): orderId={} queda PENDIENTE para reconciliación",
                        e.getStatusCode(), event.orderId());
            }
        } catch (Exception e) {
            // Timeout o circuit breaker abierto: no sabemos si Wompi procesó la transacción
            // → dejamos PENDIENTE para que el job de reconciliación resuelva el estado
            log.warn("Fallo de conectividad con Wompi para orderId={}: {} - queda PENDIENTE para reconciliación",
                    event.orderId(), e.getMessage());
        }
    }
}
