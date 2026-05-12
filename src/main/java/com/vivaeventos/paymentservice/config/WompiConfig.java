package com.vivaeventos.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment.gateway")
@Data
public class WompiConfig {
    private String baseUrl;
    private String publicKey;
    private String privateKey;
    private String webhookSecret;
    private int timeoutSeconds = 30;
}
