package com.vivaeventos.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_webhooks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhook {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private String eventType;

    @Column(columnDefinition = "jsonb")
    private String rawPayload;

    private Boolean processed;

    private LocalDateTime receivedAt;
}
