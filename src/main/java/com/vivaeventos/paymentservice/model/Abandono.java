package com.vivaeventos.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "abandonos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abandono {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "amount_in_cents")
    private Long amountInCents;

    @Column(name = "minutes_pending")
    private Integer minutesPending;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;
}
