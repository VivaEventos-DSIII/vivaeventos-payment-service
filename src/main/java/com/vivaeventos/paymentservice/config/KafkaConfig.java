package com.vivaeventos.paymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

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
}
