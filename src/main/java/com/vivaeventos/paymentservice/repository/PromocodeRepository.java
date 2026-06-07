package com.vivaeventos.paymentservice.repository;

import com.vivaeventos.paymentservice.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromocodeRepository extends JpaRepository<PromoCode, UUID> {
    
    /**
     * Busca un código promocional por su código único
     * @param code el código a buscar
     * @return Optional con el PromoCode si existe
     */
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    
    /**
     * Verifica si un código existe (case-insensitive)
     * @param code el código a verificar
     * @return true si existe, false en otro caso
     */
    boolean existsByCodeIgnoreCase(String code);
}

