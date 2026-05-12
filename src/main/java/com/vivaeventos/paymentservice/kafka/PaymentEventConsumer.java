package com.vivaeventos.paymentservice.kafka;

import com.vivaeventos.paymentservice.dto.OrdenListaParaPagoEvent;
import com.vivaeventos.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "orden-lista-para-pago",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrdenListaParaPago(
            @Payload OrdenListaParaPagoEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Evento recibido: topic={} partition={} offset={} orderId={}",
                topic, partition, offset, event.orderId());
        try {
            paymentService.initiatePago(event);
        } catch (Exception e) {
            log.error("Error procesando orden-lista-para-pago: orderId={} error={}", event.orderId(), e.getMessage(), e);
        }
    }
}
