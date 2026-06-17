package com.vivaeventos.paymentservice.service;

import com.vivaeventos.paymentservice.dto.ApplyPromocodeRequestDto;
import com.vivaeventos.paymentservice.dto.ApplyPromocodeResponseDto;
import com.vivaeventos.paymentservice.dto.ValidatePromocodeResponseDto;
import com.vivaeventos.paymentservice.exception.PromocodeException;
import com.vivaeventos.paymentservice.model.PromoCode;
import com.vivaeventos.paymentservice.repository.PromocodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromocodeServiceTest {

    @Mock
    private PromocodeRepository promocodeRepository;

    @InjectMocks
    private PromocodeService promocodeService;

    private PromoCode buildValidPercenctagePromoCode() {
        return PromoCode.builder()
                .id(UUID.randomUUID())
                .code("SUMMER20")
                .discountType(PromoCode.DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .active(true)
                .expirationDate(LocalDateTime.now().plusMonths(6))
                .usageLimit(1000)
                .usedCount(150)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PromoCode buildValidFixedPromoCode() {
        return PromoCode.builder()
                .id(UUID.randomUUID())
                .code("FIXED100K")
                .discountType(PromoCode.DiscountType.FIXED)
                .discountValue(new BigDecimal("100000.00"))
                .active(true)
                .expirationDate(LocalDateTime.now().plusMonths(6))
                .usageLimit(null)
                .usedCount(10)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ======== Validación Tests ========

    @Test
    void dadoCodigoValido_cuandoSeValida_entoncesRetornaDetallesDelCodigo() {
        PromoCode promoCode = buildValidPercenctagePromoCode();
        when(promocodeRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(promoCode));

        ValidatePromocodeResponseDto response = promocodeService.validarCodigo("SUMMER20");

        assertNotNull(response);
        assertEquals("SUMMER20", response.code());
        assertTrue(response.valid());
        assertEquals("Código válido y activo", response.message());
        assertEquals("PERCENTAGE", response.discountType());
        assertEquals(new BigDecimal("20.00"), response.discountValue());
    }

    @Test
    void dadoCodigoNoExistente_cuandoSeValida_entoncesLanzaExcepcion() {
        when(promocodeRepository.findByCodeIgnoreCase("INVALIDO")).thenReturn(Optional.empty());

        assertThrows(PromocodeException.class, () -> promocodeService.validarCodigo("INVALIDO"));
    }

    @Test
    void dadoCodigoVacio_cuandoSeValida_entoncesLanzaExcepcion() {
        assertThrows(PromocodeException.class, () -> promocodeService.validarCodigo(""));
    }

    @Test
    void dadoCodigoInactivo_cuandoSeValida_entoncesMarkaComoNoValido() {
        PromoCode inactiveCode = buildValidPercenctagePromoCode();
        inactiveCode.setActive(false);
        when(promocodeRepository.findByCodeIgnoreCase("INACTIVE")).thenReturn(Optional.of(inactiveCode));

        ValidatePromocodeResponseDto response = promocodeService.validarCodigo("INACTIVE");

        assertFalse(response.valid());
        assertEquals("El código promocional está inactivo", response.message());
    }

    @Test
    void dadoCodigoExpirado_cuandoSeValida_entoncesMarkaComoNoValido() {
        PromoCode expiredCode = buildValidPercenctagePromoCode();
        expiredCode.setExpirationDate(LocalDateTime.now().minusDays(1));
        when(promocodeRepository.findByCodeIgnoreCase("EXPIRED")).thenReturn(Optional.of(expiredCode));

        ValidatePromocodeResponseDto response = promocodeService.validarCodigo("EXPIRED");

        assertFalse(response.valid());
        assertEquals("El código promocional ha expirado", response.message());
    }

    @Test
    void dadoCodigoConLimiteDeUsosAlcanzado_cuandoSeValida_entoncesMarkaComoNoValido() {
        PromoCode limitedCode = buildValidPercenctagePromoCode();
        limitedCode.setUsageLimit(100);
        limitedCode.setUsedCount(100);
        when(promocodeRepository.findByCodeIgnoreCase("LIMITED")).thenReturn(Optional.of(limitedCode));

        ValidatePromocodeResponseDto response = promocodeService.validarCodigo("LIMITED");

        assertFalse(response.valid());
        assertEquals("El código promocional ha alcanzado su límite de usos", response.message());
    }

    // ======== Aplicación de Descuento Tests ========

    @Test
    void dadoCodigoPercentual_cuandoSeAplica_entoncesCalculaDescuentoCorrectamente() {
        PromoCode promoCode = buildValidPercenctagePromoCode();
        when(promocodeRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(promoCode));
        when(promocodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BigDecimal originalAmount = new BigDecimal("500000.00");
        ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                "SUMMER20",
                UUID.randomUUID(),
                originalAmount
        );

        ApplyPromocodeResponseDto response = promocodeService.aplicarCodigo(request);

        // Descuento esperado: 500000 * 20 / 100 = 100000
        assertEquals(new BigDecimal("100000.00"), response.discountApplied());
        assertEquals(new BigDecimal("400000.00"), response.newTotal());
        assertEquals(40000000L, response.newTotalInCents());
    }

    @Test
    void dadoCodigoFijo_cuandoSeAplica_entoncesAplicaDescuentoFijo() {
        PromoCode promoCode = buildValidFixedPromoCode();
        when(promocodeRepository.findByCodeIgnoreCase("FIXED100K")).thenReturn(Optional.of(promoCode));
        when(promocodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BigDecimal originalAmount = new BigDecimal("500000.00");
        ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                "FIXED100K",
                UUID.randomUUID(),
                originalAmount
        );

        ApplyPromocodeResponseDto response = promocodeService.aplicarCodigo(request);

        assertEquals(new BigDecimal("100000.00"), response.discountApplied());
        assertEquals(new BigDecimal("400000.00"), response.newTotal());
    }

    @Test
    void dadoDescuentoFijoMayorAlMonto_cuandoSeAplica_entoncesLimitaAlMonto() {
        PromoCode promoCode = buildValidFixedPromoCode();
        when(promocodeRepository.findByCodeIgnoreCase("FIXED100K")).thenReturn(Optional.of(promoCode));
        when(promocodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BigDecimal smallAmount = new BigDecimal("50000.00");
        ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                "FIXED100K",
                UUID.randomUUID(),
                smallAmount
        );

        ApplyPromocodeResponseDto response = promocodeService.aplicarCodigo(request);

        // El descuento no puede ser mayor al monto original
        assertEquals(new BigDecimal("50000.00"), response.discountApplied());
        assertEquals(BigDecimal.ZERO, response.newTotal());
    }

    @Test
    void dadoCodigoInvalidoAlAplicar_cuandoSeAplica_entoncesLanzaExcepcion() {
        PromoCode inactiveCode = buildValidPercenctagePromoCode();
        inactiveCode.setActive(false);
        when(promocodeRepository.findByCodeIgnoreCase("INACTIVE")).thenReturn(Optional.of(inactiveCode));

        ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                "INACTIVE",
                UUID.randomUUID(),
                new BigDecimal("500000.00")
        );

        assertThrows(PromocodeException.class, () -> promocodeService.aplicarCodigo(request));
    }

    @Test
    void dadoMontoNegativoOCero_cuandoSeAplica_entoncesLanzaExcepcion() {
        ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                "SUMMER20",
                UUID.randomUUID(),
                BigDecimal.ZERO
        );

        assertThrows(PromocodeException.class, () -> promocodeService.aplicarCodigo(request));
    }

    @Test
    void dadoCodigoValido_cuandoSeAplica_entoncesIncrementaContadorDeUsos() {
        PromoCode promoCode = buildValidPercenctagePromoCode();
        int initialCount = promoCode.getUsedCount();
        when(promocodeRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(promoCode));
        when(promocodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                "SUMMER20",
                UUID.randomUUID(),
                new BigDecimal("500000.00")
        );

        promocodeService.aplicarCodigo(request);

        ArgumentCaptor<PromoCode> captor = ArgumentCaptor.forClass(PromoCode.class);
        verify(promocodeRepository).save(captor.capture());
        PromoCode saved = captor.getValue();
        assertEquals(initialCount + 1, saved.getUsedCount());
    }

    @Test
    void dadoCodigoConCaseMixto_cuandoSeValida_entoncesLoEnCuentraCorrectamente() {
        PromoCode promoCode = buildValidPercenctagePromoCode();
        when(promocodeRepository.findByCodeIgnoreCase("summer20")).thenReturn(Optional.of(promoCode));

        ValidatePromocodeResponseDto response = promocodeService.validarCodigo("summer20");

        assertNotNull(response);
        assertEquals("SUMMER20", response.code());
    }

    // ======== Activación y Desactivación Tests (US-29) ========

    @Test
    void dadoCodigoInactivo_cuandoSeActiva_entoncesQuedaActivo() {
        UUID promocodeId = UUID.randomUUID();
        PromoCode inactiveCode = buildValidPercenctagePromoCode();
        inactiveCode.setId(promocodeId);
        inactiveCode.setActive(false);
        
        when(promocodeRepository.findById(promocodeId)).thenReturn(Optional.of(inactiveCode));
        when(promocodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ValidatePromocodeResponseDto response = promocodeService.activarCodigo(promocodeId);

        assertTrue(response.active());
        assertEquals("Código activado correctamente", response.message());
        verify(promocodeRepository).save(any());
    }

    @Test
    void dadoCodigoActivoExistente_cuandoSeActiva_entoncesSeMantieneActivo() {
        UUID promocodeId = UUID.randomUUID();
        PromoCode activeCode = buildValidPercenctagePromoCode();
        activeCode.setId(promocodeId);
        activeCode.setActive(true);
        
        when(promocodeRepository.findById(promocodeId)).thenReturn(Optional.of(activeCode));

        ValidatePromocodeResponseDto response = promocodeService.activarCodigo(promocodeId);

        assertTrue(response.active());
        assertEquals("Código activado correctamente", response.message());
    }

    @Test
    void dadoCodigoActivoExistente_cuandoSeDesactiva_entoncesQuedaInactivo() {
        UUID promocodeId = UUID.randomUUID();
        PromoCode activeCode = buildValidPercenctagePromoCode();
        activeCode.setId(promocodeId);
        activeCode.setActive(true);
        
        when(promocodeRepository.findById(promocodeId)).thenReturn(Optional.of(activeCode));
        when(promocodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ValidatePromocodeResponseDto response = promocodeService.desactivarCodigo(promocodeId);

        assertFalse(response.active());
        assertEquals("Código desactivado correctamente", response.message());
        verify(promocodeRepository).save(any());
    }

    @Test
    void dadoCodigoInactivo_cuandoSeDesactiva_entoncesSeMantieneInactivo() {
        UUID promocodeId = UUID.randomUUID();
        PromoCode inactiveCode = buildValidPercenctagePromoCode();
        inactiveCode.setId(promocodeId);
        inactiveCode.setActive(false);
        
        when(promocodeRepository.findById(promocodeId)).thenReturn(Optional.of(inactiveCode));

        ValidatePromocodeResponseDto response = promocodeService.desactivarCodigo(promocodeId);

        assertFalse(response.active());
        assertEquals("Código desactivado correctamente", response.message());
    }

    @Test
    void dadoCodigoInexistente_cuandoSeActiva_entoncesLanzaExcepcion() {
        UUID promocodeId = UUID.randomUUID();
        when(promocodeRepository.findById(promocodeId)).thenReturn(Optional.empty());

        assertThrows(PromocodeException.class, () -> promocodeService.activarCodigo(promocodeId));
    }

    @Test
    void dadoCodigoInexistente_cuandoSeDesactiva_entoncesLanzaExcepcion() {
        UUID promocodeId = UUID.randomUUID();
        when(promocodeRepository.findById(promocodeId)).thenReturn(Optional.empty());

        assertThrows(PromocodeException.class, () -> promocodeService.desactivarCodigo(promocodeId));
    }

    @Test
    void dadoCodigoConLimiteAlcanzado_cuandoSeAplica_entoncesSeDesactivaAutomaticamente() {
        PromoCode promoCode = buildValidPercenctagePromoCode();
        promoCode.setUsageLimit(100);
        promoCode.setUsedCount(99);
        promoCode.setActive(true);
        
        when(promocodeRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(promoCode));
        when(promocodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                "SUMMER20",
                UUID.randomUUID(),
                new BigDecimal("500000.00")
        );

        ApplyPromocodeResponseDto response = promocodeService.aplicarCodigo(request);

        assertNotNull(response);
        assertEquals("Descuento aplicado exitosamente", response.message());
        
        ArgumentCaptor<PromoCode> captor = ArgumentCaptor.forClass(PromoCode.class);
        verify(promocodeRepository).save(captor.capture());
        PromoCode saved = captor.getValue();
        assertEquals(100, saved.getUsedCount());
        assertFalse(saved.getActive()); // US-29 Escenario 3: Auto-desactivado
    }

    @Test
    void dadoCodigoConDesactivacionAutomatica_cuandoSeIntentaUsarDeNuevo_entoncesLanzaExcepcion() {
        PromoCode promoCode = buildValidPercenctagePromoCode();
        promoCode.setUsageLimit(100);
        promoCode.setUsedCount(100);
        promoCode.setActive(false); // Desactivado automáticamente
        
        when(promocodeRepository.findByCodeIgnoreCase("SUMMER20")).thenReturn(Optional.of(promoCode));

        ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                "SUMMER20",
                UUID.randomUUID(),
                new BigDecimal("500000.00")
        );

        assertThrows(PromocodeException.class, () -> promocodeService.aplicarCodigo(request));
    }

    @Test
    void dadoCodigoValido_cuandoSeObtiene_entoncesRetornaDetallesCompletos() {
        UUID promocodeId = UUID.randomUUID();
        PromoCode promoCode = buildValidPercenctagePromoCode();
        promoCode.setId(promocodeId);
        
        when(promocodeRepository.findById(promocodeId)).thenReturn(Optional.of(promoCode));

        ValidatePromocodeResponseDto response = promocodeService.obtenerCodigo(promocodeId);

        assertNotNull(response);
        assertEquals(promocodeId, response.promocodeId());
        assertEquals("SUMMER20", response.code());
        assertTrue(response.active());
        assertEquals(new BigDecimal("20.00"), response.discountValue());
    }

    @Test
    void dadoCodigoInexistente_cuandoSeObtiene_entoncesLanzaExcepcion() {
        UUID promocodeId = UUID.randomUUID();
        when(promocodeRepository.findById(promocodeId)).thenReturn(Optional.empty());

        assertThrows(PromocodeException.class, () -> promocodeService.obtenerCodigo(promocodeId));
    }
}


