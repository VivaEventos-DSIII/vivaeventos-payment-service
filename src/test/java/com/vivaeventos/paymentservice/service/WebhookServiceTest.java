package com.vivaeventos.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivaeventos.paymentservice.dto.WompiWebhookRequest;
import com.vivaeventos.paymentservice.kafka.PaymentEventPublisher;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentStatus;
import com.vivaeventos.paymentservice.repository.PaymentRepository;
import com.vivaeventos.paymentservice.repository.PaymentWebhookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentWebhookRepository webhookRepository;
    @Mock private PaymentEventPublisher eventPublisher;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private WebhookService webhookService;

    private Payment buildPayment() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setReference("ref-001");
        payment.setStatus(PaymentStatus.PENDIENTE);
        payment.setAmountInCents(100000L);
        payment.setCustomerEmail("test@example.com");
        return payment;
    }

    private WompiWebhookRequest buildWebhookRequest(String txnStatus) {
        WompiWebhookRequest.Transaction txn = new WompiWebhookRequest.Transaction(
                "txn-001", txnStatus, "ref-001", 100000L, "COP", "CARD");
        WompiWebhookRequest.Data data = new WompiWebhookRequest.Data(txn);
        WompiWebhookRequest.Signature sig = new WompiWebhookRequest.Signature(
                List.of(), "checksum", 1700000000L);
        return new WompiWebhookRequest("transaction.updated", data, "2024-01-01T00:00:00Z", sig);
    }

    @Test
    void processWebhook_whenApproved_setsAprobadoAndPublishesEvent() throws Exception {
        when(webhookRepository.existsByPayment_WompiTransactionIdAndEventType(any(), any())).thenReturn(false);
        when(paymentRepository.findByReference("ref-001")).thenReturn(Optional.of(buildPayment()));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        webhookService.processWebhook(buildWebhookRequest("APPROVED"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(PaymentStatus.APROBADO, captor.getValue().getStatus());
        verify(eventPublisher).publishPagoConfirmado(any());
        verify(eventPublisher, never()).publishPagoFallido(any(), any());
    }

    @Test
    void processWebhook_whenPending_keepsPendienteWithNoEventPublished() throws Exception {
        when(webhookRepository.existsByPayment_WompiTransactionIdAndEventType(any(), any())).thenReturn(false);
        when(paymentRepository.findByReference("ref-001")).thenReturn(Optional.of(buildPayment()));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        webhookService.processWebhook(buildWebhookRequest("PENDING"));

        // El estado del pago no cambia y el webhook se guarda para auditoría
        verify(paymentRepository, never()).save(any());
        verify(webhookRepository).save(any());
        verify(eventPublisher, never()).publishPagoConfirmado(any());
        verify(eventPublisher, never()).publishPagoFallido(any(), any());
    }

    @Test
    void processWebhook_whenDeclined_setsDeclinadoAndPublishesFailEvent() throws Exception {
        when(webhookRepository.existsByPayment_WompiTransactionIdAndEventType(any(), any())).thenReturn(false);
        when(paymentRepository.findByReference("ref-001")).thenReturn(Optional.of(buildPayment()));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        webhookService.processWebhook(buildWebhookRequest("DECLINED"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(PaymentStatus.DECLINADO, captor.getValue().getStatus());
        verify(eventPublisher).publishPagoFallido(any(), any());
        verify(eventPublisher, never()).publishPagoConfirmado(any());
    }

    @Test
    void processWebhook_whenVoided_setsFallidoAndPublishesFailEvent() throws Exception {
        when(webhookRepository.existsByPayment_WompiTransactionIdAndEventType(any(), any())).thenReturn(false);
        when(paymentRepository.findByReference("ref-001")).thenReturn(Optional.of(buildPayment()));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        webhookService.processWebhook(buildWebhookRequest("VOIDED"));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(PaymentStatus.FALLIDO, captor.getValue().getStatus());
        verify(eventPublisher).publishPagoFallido(any(), any());
    }

    @Test
    void processWebhook_whenPaymentNotFound_savesWebhookForAudit() throws Exception {
        when(webhookRepository.existsByPayment_WompiTransactionIdAndEventType(any(), any())).thenReturn(false);
        when(paymentRepository.findByReference("ref-001")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        webhookService.processWebhook(buildWebhookRequest("APPROVED"));

        verify(webhookRepository).save(any());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void processWebhook_whenDuplicateWebhook_skipsProcessing() {
        when(webhookRepository.existsByPayment_WompiTransactionIdAndEventType(any(), any())).thenReturn(true);

        webhookService.processWebhook(buildWebhookRequest("APPROVED"));

        verify(paymentRepository, never()).findByReference(any());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}
