package com.vivaeventos.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "ticket_type_id")
    private UUID ticketTypeId;

    @Column(name = "wompi_transaction_id")
    private String wompiTransactionId;

    @Column(name = "reference", nullable = false, unique = true)
    private String reference;

    @Column(name = "amount_in_cents", nullable = false)
    private Long amountInCents;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "COP";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDIENTE;

    @Column(name = "payment_method_type")
    private String paymentMethodType;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
