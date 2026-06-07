package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.config.WompiConfig;
import com.vivaeventos.paymentservice.event.CompraAbandonadaEvent;
import com.vivaeventos.paymentservice.model.Abandono;
import com.vivaeventos.paymentservice.model.Payment;
import com.vivaeventos.paymentservice.model.PaymentStatus;
import com.vivaeventos.paymentservice.repository.AbandonoRepository;
import com.vivaeventos.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbandonoServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AbandonoRepository abandonoRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private WompiConfig wompiConfig;

    @InjectMocks
    private AbandonoService abandonoService;

    private Payment buildPaymentPendiente() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerEmail("cliente@example.com")
                .amountInCents(150000L)
                .status(PaymentStatus.PENDIENTE)
                .reference("ref-test-001")
                .createdAt(LocalDateTime.now().minusMinutes(35))
                .updatedAt(LocalDateTime.now().minusMinutes(35))
                .build();
    }

    @Test
    void dadoPagoViejoSinRegistroPrevio_cuandoDetectarAbandonos_entoncesSeGuardaYPublicaEvento() {
        when(wompiConfig.getAbandonmentThresholdMinutes()).thenReturn(30);
        Payment pago = buildPaymentPendiente();
        when(paymentRepository.findByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDIENTE), any()))
                .thenReturn(List.of(pago));
        when(abandonoRepository.existsByPayment_Id(pago.getId())).thenReturn(false);
        when(abandonoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        abandonoService.detectarAbandonos();

        ArgumentCaptor<Abandono> abandonoCaptor = ArgumentCaptor.forClass(Abandono.class);
        verify(abandonoRepository).save(abandonoCaptor.capture());
        Abandono guardado = abandonoCaptor.getValue();
        assertEquals(pago.getOrderId(), guardado.getOrderId());
        assertEquals(pago.getCustomerEmail(), guardado.getCustomerEmail());
        assertEquals(pago.getAmountInCents(), guardado.getAmountInCents());
        assertTrue(guardado.getMinutesPending() >= 35);
        assertNotNull(guardado.getDetectedAt());

        ArgumentCaptor<CompraAbandonadaEvent> eventoCaptor = ArgumentCaptor.forClass(CompraAbandonadaEvent.class);
        verify(kafkaTemplate).send(eq("compra-abandonada"), eq(pago.getOrderId().toString()), eventoCaptor.capture());
        CompraAbandonadaEvent evento = eventoCaptor.getValue();
        assertEquals(pago.getOrderId(), evento.orderId());
        assertEquals(pago.getId(), evento.paymentId());
        assertEquals(pago.getCustomerEmail(), evento.customerEmail());
        assertEquals(pago.getAmountInCents(), evento.amountInCents());
    }

    @Test
    void dadoPagoViejoYaRegistrado_cuandoDetectarAbandonos_entoncesSeOmite() {
        when(wompiConfig.getAbandonmentThresholdMinutes()).thenReturn(30);
        Payment pago = buildPaymentPendiente();
        when(paymentRepository.findByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDIENTE), any()))
                .thenReturn(List.of(pago));
        when(abandonoRepository.existsByPayment_Id(pago.getId())).thenReturn(true);

        abandonoService.detectarAbandonos();

        verify(abandonoRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void dadoSinPagosViejos_cuandoDetectarAbandonos_entoncesNoSeRegistraNada() {
        when(wompiConfig.getAbandonmentThresholdMinutes()).thenReturn(30);
        when(paymentRepository.findByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDIENTE), any()))
                .thenReturn(Collections.emptyList());

        abandonoService.detectarAbandonos();

        verify(abandonoRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
