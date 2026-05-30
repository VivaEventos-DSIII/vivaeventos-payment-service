package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.dto.OrdenListaParaPagoEvent;
import com.vivaeventos.paymentservice.dto.WompiTransactionResponse;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentStatus;
import com.vivaeventos.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private WompiClient wompiClient;
    @InjectMocks
    private PaymentService paymentService;

    private OrdenListaParaPagoEvent buildEvent() {
        return new OrdenListaParaPagoEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@example.com",
                "Carlos López",
                250000L,
                "tok_test_visa_1234"
        );
    }

    @Test
    void initiatePago_whenGatewayTimeout_paymentRemainsInPendiente() {
        when(paymentRepository.existsByReference(any())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(wompiClient.getAcceptanceToken()).thenReturn("tok-acceptance");
        when(wompiClient.createTransaction(any()))
                .thenThrow(new RuntimeException("Pasarela de pagos no disponible. Intente más tarde."));

        paymentService.initiatePago(buildEvent());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        // Solo el save inicial; no se debe guardar de nuevo con FALLIDO
        verify(paymentRepository, times(1)).save(captor.capture());
        assertEquals(PaymentStatus.PENDIENTE, captor.getValue().getStatus());
    }

    @Test
    void initiatePago_when4xxFromGateway_paymentSetToFallido() {
        when(paymentRepository.existsByReference(any())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(wompiClient.getAcceptanceToken()).thenReturn("tok-acceptance");
        WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity",
                HttpHeaders.EMPTY, null, StandardCharsets.UTF_8);
        when(wompiClient.createTransaction(any())).thenThrow(ex);

        paymentService.initiatePago(buildEvent());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        List<Payment> saved = captor.getAllValues();
        assertEquals(PaymentStatus.PENDIENTE, saved.get(0).getStatus());
        assertEquals(PaymentStatus.FALLIDO, saved.get(1).getStatus());
    }

    @Test
    void initiatePago_whenGateway5xx_paymentRemainsInPendiente() {
        when(paymentRepository.existsByReference(any())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(wompiClient.getAcceptanceToken()).thenReturn("tok-acceptance");
        WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable",
                HttpHeaders.EMPTY, null, StandardCharsets.UTF_8);
        when(wompiClient.createTransaction(any())).thenThrow(ex);

        paymentService.initiatePago(buildEvent());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        assertEquals(PaymentStatus.PENDIENTE, captor.getValue().getStatus());
    }

    @Test
    void initiatePago_whenSuccess_paymentHasWompiTransactionId() {
        when(paymentRepository.existsByReference(any())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(wompiClient.getAcceptanceToken()).thenReturn("tok-acceptance");
        WompiTransactionResponse.Data data = new WompiTransactionResponse.Data(
                "txn-wompi-001", "PENDING", "ref", 250000L, "COP", "CARD");
        when(wompiClient.createTransaction(any())).thenReturn(new WompiTransactionResponse(data));

        paymentService.initiatePago(buildEvent());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        Payment finalPayment = captor.getAllValues().get(1);
        assertEquals("txn-wompi-001", finalPayment.getWompiTransactionId());
        assertEquals(PaymentStatus.PENDIENTE, finalPayment.getStatus());
    }

    @Test
    void initiatePago_whenAcceptanceTokenFails_paymentSetToFallido() {
        when(paymentRepository.existsByReference(any())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(wompiClient.getAcceptanceToken())
                .thenThrow(new RuntimeException("Pasarela de pagos no disponible. Intente más tarde."));

        paymentService.initiatePago(buildEvent());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        assertEquals(PaymentStatus.FALLIDO, captor.getAllValues().get(1).getStatus());
        verify(wompiClient, never()).createTransaction(any());
    }

    @Test
    void initiatePago_whenDuplicateOrder_skipsProcessing() {
        when(paymentRepository.existsByReference(any())).thenReturn(true);

        paymentService.initiatePago(buildEvent());

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(wompiClient);
    }
}
