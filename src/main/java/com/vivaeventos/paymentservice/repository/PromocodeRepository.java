package com.vivaeventos.paymentservice.repository;

import com.vivaeventos.paymentservice.model.PromoCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromocodeRepository extends JpaRepository<PromoCode, UUID> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromoCode p WHERE LOWER(p.code) = LOWER(:code)")
    Optional<PromoCode> findByCodeIgnoreCaseForUpdate(@Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);
}

