package com.vivaeventos.paymentservice.repository;

import com.vivaeventos.paymentservice.model.Abandono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AbandonoRepository extends JpaRepository<Abandono, UUID> {
    boolean existsByPayment_Id(UUID paymentId);
}
