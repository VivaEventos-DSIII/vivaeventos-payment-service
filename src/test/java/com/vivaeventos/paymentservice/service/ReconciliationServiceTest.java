package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.config.WompiConfig;
import com.vivaeventos.paymentservice.dto.WompiTransactionResponse;
import com.vivaeventos.paymentservice.kafka.PaymentEventPublisher;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentStatus;
import com.vivaeventos.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private WompiClient wompiClient;
    @Mock private PaymentEventPublisher eventPublisher;
    @Mock private WompiConfig wompiConfig;
    @InjectMocks private ReconciliationService reconciliationService;

    private Payment buildPendingPayment(String wompiId) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setReference("ref-001");
        payment.setStatus(PaymentStatus.PENDIENTE);
        payment.setAmountInCents(100000L);
        payment.setCustomerEmail("test@example.com");
        payment.setWompiTransactionId(wompiId);
        return payment;
    }

    private WompiTransactionResponse buildWompiResponse(String status) {
        WompiTransactionResponse.Data data = new WompiTransactionResponse.Data(
                "txn-001", status, "ref-001", 100000L, "COP", "CARD");
        return new WompiTransactionResponse(data);
    }

    @Test
    void reconciliar_whenWompiApproved_updatesStatusAndPublishesConfirmedEvent() {
        when(wompiConfig.getPendingThresholdMinutes()).thenReturn(3);
        Payment payment = buildPendingPayment("txn-001");
        when(paymentRepository.findByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDIENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(payment));
        when(wompiClient.getTransaction("txn-001")).thenReturn(buildWompiResponse("APPROVED"));

        reconciliationService.reconciliarPagosPendientes();

        assertEquals(PaymentStatus.APROBADO, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishPagoConfirmado(payment);
        verify(eventPublisher, never()).publishPagoFallido(any(), any());
    }

    @Test
    void reconciliar_whenWompiDeclined_updatesStatusAndPublishesFailEvent() {
        when(wompiConfig.getPendingThresholdMinutes()).thenReturn(3);
        Payment payment = buildPendingPayment("txn-001");
        when(paymentRepository.findByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDIENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(payment));
        when(wompiClient.getTransaction("txn-001")).thenReturn(buildWompiResponse("DECLINED"));

        reconciliationService.reconciliarPagosPendientes();

        assertEquals(PaymentStatus.DECLINADO, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishPagoFallido(eq(payment), any());
    }

    @Test
    void reconciliar_whenWompiStillPending_noStateChange() {
        when(wompiConfig.getPendingThresholdMinutes()).thenReturn(3);
        Payment payment = buildPendingPayment("txn-001");
        when(paymentRepository.findByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDIENTE), any(LocalDateTime.class)))
                .thenReturn(List.of(payment));
        when(wompiClient.getTransaction("txn-001")).thenReturn(buildWompiResponse("PENDING"));

        reconciliationService.reconciliarPagosPendientes();

        assertEquals(PaymentStatus.PENDIENTE, payment.getStatus());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void reconciliar_whenNoPendingPayments_noCallsToWompi() {
        when(wompiConfig.getPendingThresholdMinutes()).thenReturn(3);
        when(paymentRepository.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of());

        reconciliationService.reconciliarPagosPendientes();

        verifyNoInteractions(wompiClient, eventPublisher);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void reconciliar_whenWompiCallFails_paymentRemainsUnchangedAndContinues() {
        when(wompiConfig.getPendingThresholdMinutes()).thenReturn(3);
        Payment payment = buildPendingPayment("txn-001");
        when(paymentRepository.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(payment));
        when(wompiClient.getTransaction("txn-001")).thenThrow(new RuntimeException("Gateway error"));

        reconciliationService.reconciliarPagosPendientes();

        assertEquals(PaymentStatus.PENDIENTE, payment.getStatus());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void reconciliar_whenPaymentExceedsMaxAge_failsAndPublishesFailEvent() {
        when(wompiConfig.getPendingThresholdMinutes()).thenReturn(3);
        when(wompiConfig.getMaxPendingAgeHours()).thenReturn(24);
        Payment payment = buildPendingPayment("txn-001");
        payment.setCreatedAt(LocalDateTime.now().minusHours(25));
        when(paymentRepository.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(payment));
        when(wompiClient.getTransaction("txn-001")).thenReturn(buildWompiResponse("PENDING"));

        reconciliationService.reconciliarPagosPendientes();

        assertEquals(PaymentStatus.FALLIDO, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishPagoFallido(eq(payment), contains("Tiempo máximo"));
    }

    @Test
    void reconciliar_whenNoWompiId_queriesByReferenceInsteadOfById() {
        when(wompiConfig.getPendingThresholdMinutes()).thenReturn(3);
        Payment payment = buildPendingPayment(null);
        when(paymentRepository.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of(payment));
        when(wompiClient.getTransactionByReference("ref-001")).thenReturn(buildWompiResponse("APPROVED"));

        reconciliationService.reconciliarPagosPendientes();

        assertEquals(PaymentStatus.APROBADO, payment.getStatus());
        verify(wompiClient, never()).getTransaction(any());
        verify(wompiClient).getTransactionByReference("ref-001");
        verify(eventPublisher).publishPagoConfirmado(payment);
    }
}
