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
    @GeneratedValue
    private UUID id;

    private UUID orderId;

    private String gatewayPaymentId;

    private Double amount;

    private String currency;

    private String status;

    @Column(columnDefinition = "jsonb")
    private String gatewayResponse;

    @Column(unique = true)
    private String idempotencyKey;

    private LocalDateTime initiatedAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime failedAt;

    private LocalDateTime updatedAt;
}
