package com.vivaeventos.paymentservice.repository;

import com.vivaeventos.paymentservice.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
}
