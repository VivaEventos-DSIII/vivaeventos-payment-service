package com.vivaeventos.paymentservice.repository;

import com.vivaeventos.paymentservice.model.PaymentWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, UUID> {
    boolean existsByPayment_GatewayPaymentIdAndEventType(String gatewayPaymentId, String eventType);
}
