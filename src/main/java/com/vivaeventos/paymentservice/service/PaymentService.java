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

        try {
            String acceptanceToken = wompiClient.getAcceptanceToken();

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

            WompiTransactionResponse response = wompiClient.createTransaction(transactionRequest);

            if (response != null && response.data() != null) {
                payment.setWompiTransactionId(response.data().id());
                payment.setPaymentMethodType(response.data().paymentMethodType());
                paymentRepository.save(payment);
                log.info("Transacción Wompi creada: wompiId={} status={}", response.data().id(), response.data().status());
            }

        } catch (Exception e) {
            log.error("Error al crear transacción en Wompi para orderId={}: {}", event.orderId(), e.getMessage());
            payment.setStatus(PaymentStatus.FALLIDO);
            paymentRepository.save(payment);
        }
    }
}
