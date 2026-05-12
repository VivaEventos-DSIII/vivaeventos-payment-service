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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private String rawPayload;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    void prePersist() {
        if (receivedAt == null) receivedAt = LocalDateTime.now();
    }
}
