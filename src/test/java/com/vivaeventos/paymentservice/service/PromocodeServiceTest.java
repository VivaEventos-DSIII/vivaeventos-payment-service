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
}


