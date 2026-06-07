package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.dto.DevolucionSolicitadaEvent;
import com.vivaeventos.paymentservice.dto.RefundRequestDto;
import com.vivaeventos.paymentservice.dto.RefundResponseDto;
import com.vivaeventos.paymentservice.model.Refund;
import com.vivaeventos.paymentservice.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private RefundService refundService;

    private RefundRequestDto buildRequest() {
        return new RefundRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cliente@example.com",
                "Carlos López",
                new BigDecimal("240000.00"),
                "EVENTO_CANCELADO"
        );
    }

    @Test
    void dadoSolicitudValida_cuandoSeRegistraDevolucion_entoncesSeGuardaEnBD() {
        when(refundRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        refundService.registrarDevolucion(buildRequest());

        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(captor.capture());
        Refund saved = captor.getValue();
        assertEquals("REQUESTED", saved.getStatus());
        assertEquals("EVENTO_CANCELADO", saved.getReason());
        assertNotNull(saved.getRequestedAt());
    }

    @Test
    void dadoSolicitudValida_cuandoSeRegistraDevolucion_entoncesSePublicaEventoKafka() {
        when(refundRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RefundRequestDto request = buildRequest();
        refundService.registrarDevolucion(request);

        ArgumentCaptor<DevolucionSolicitadaEvent> eventCaptor =
                ArgumentCaptor.forClass(DevolucionSolicitadaEvent.class);
        verify(kafkaTemplate).send(
                eq("order.refund-requested"),
                eq(request.orderId().toString()),
                eventCaptor.capture()
        );

        DevolucionSolicitadaEvent event = eventCaptor.getValue();
        assertEquals(request.orderId(), event.orderId());
        assertEquals(request.eventId(), event.eventId());
        assertEquals(request.customerId(), event.customerId());
        assertEquals(request.userEmail(), event.userEmail());
        assertEquals(request.userName(), event.userName());
        assertEquals(request.totalAmount(), event.totalAmount());
        assertEquals("EVENTO_CANCELADO", event.reason());
        assertNotNull(event.requestedAt());
    }

    @Test
    void dadoSolicitudValida_cuandoSeRegistraDevolucion_entoncesRetorna201ConRefundId() {
        UUID generatedId = UUID.randomUUID();
        when(refundRepository.save(any())).thenAnswer(i -> {
            Refund r = i.getArgument(0);
            // Simula el ID que JPA asigna al persistir
            return Refund.builder()
                    .id(generatedId)
                    .orderId(r.getOrderId())
                    .eventId(r.getEventId())
                    .customerId(r.getCustomerId())
                    .userEmail(r.getUserEmail())
                    .userName(r.getUserName())
                    .totalAmount(r.getTotalAmount())
                    .reason(r.getReason())
                    .status(r.getStatus())
                    .requestedAt(r.getRequestedAt())
                    .build();
        });

        RefundResponseDto response = refundService.registrarDevolucion(buildRequest());

        assertNotNull(response);
        assertEquals(generatedId, response.refundId());
        assertEquals("REQUESTED", response.status());
        assertNotNull(response.requestedAt());
    }
}
