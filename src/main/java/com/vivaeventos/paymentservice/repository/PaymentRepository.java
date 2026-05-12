package com.vivaeventos.paymentservice.repository;

import com.vivaeventos.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByReference(String reference);
    Optional<Payment> findByWompiTransactionId(String wompiTransactionId);
    boolean existsByReference(String reference);
}
