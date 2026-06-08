package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.dto.DevolucionSolicitadaEvent;
import com.vivaeventos.paymentservice.dto.RefundRequestDto;
import com.vivaeventos.paymentservice.dto.RefundResponseDto;
import com.vivaeventos.paymentservice.model.Refund;
import com.vivaeventos.paymentservice.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final String TOPIC_REFUND_REQUESTED = "order.refund-requested";

    private final RefundRepository refundRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public RefundResponseDto registrarDevolucion(RefundRequestDto request, String userEmail, UUID customerId) {
        LocalDateTime requestedAt = LocalDateTime.now();

        Refund refund = Refund.builder()
                .orderId(request.orderId())
                .eventId(request.eventId())
                .customerId(customerId)
                .userEmail(userEmail)
                .userName(request.userName())
                .totalAmount(request.totalAmount())
                .reason(request.reason() != null ? request.reason() : "EVENTO_CANCELADO")
                .status("REQUESTED")
                .requestedAt(requestedAt)
                .build();

        Refund saved = refundRepository.save(refund);
        log.info("Devolución registrada: refundId={} orderId={}", saved.getId(), saved.getOrderId());

        DevolucionSolicitadaEvent event = new DevolucionSolicitadaEvent(
                saved.getOrderId(),
                saved.getEventId(),
                saved.getCustomerId(),
                saved.getUserEmail(),
                saved.getUserName(),
                saved.getTotalAmount(),
                saved.getReason(),
                saved.getRequestedAt()
        );
        kafkaTemplate.send(TOPIC_REFUND_REQUESTED, saved.getOrderId().toString(), event);
        log.info("DevolucionSolicitada publicado en topic={}: orderId={}", TOPIC_REFUND_REQUESTED, saved.getOrderId());

        return new RefundResponseDto(saved.getId(), saved.getStatus(), saved.getRequestedAt());
    }
}
