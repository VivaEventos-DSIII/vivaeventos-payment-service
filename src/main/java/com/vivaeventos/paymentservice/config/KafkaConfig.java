package com.vivaeventos.paymentservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public NewTopic ordenListaParaPagoTopic() {
        return TopicBuilder.name("orden-lista-para-pago").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic pagoConfirmadoTopic() {
        return TopicBuilder.name("pago-confirmado").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic pagoFallidoTopic() {
        return TopicBuilder.name("pago-fallido").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic orderRefundRequestedTopic() {
        return TopicBuilder.name("order.refund-requested").partitions(1).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
