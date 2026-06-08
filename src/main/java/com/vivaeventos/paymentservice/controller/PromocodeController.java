package com.vivaeventos.paymentservice.controller;

import com.vivaeventos.paymentservice.dto.ApplyPromocodeRequestDto;
import com.vivaeventos.paymentservice.dto.ApplyPromocodeResponseDto;
import com.vivaeventos.paymentservice.dto.ValidatePromocodeResponseDto;
import com.vivaeventos.paymentservice.service.PromocodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gestión de códigos promocionales en pagos
 * US-07: Aplicar código promocional
 */
@RestController
@RequestMapping("/api/promocodes")
@RequiredArgsConstructor
public class PromocodeController {

    private final PromocodeService promocodeService;

    /**
     * Valida un código promocional sin aplicar descuento
     * 
     * @param code el código a validar
     * @return ResponseEntity con los detalles del código promocional
     * 
     * @example
     * GET /api/promocodes/validate?code=SUMMER20
     * Response: 200 OK
     * {
     *   "promocodeId": "550e8400-e29b-41d4-a716-446655440000",
     *   "code": "SUMMER20",
     *   "discountType": "PERCENTAGE",
     *   "discountValue": 20.00,
     *   "active": true,
     *   "expirationDate": "2026-12-31T23:59:59",
     *   "usageLimit": 1000,
     *   "usedCount": 150,
     *   "valid": true,
     *   "message": "Código válido y activo"
     * }
     */
    @GetMapping("/validate")
    public ResponseEntity<ValidatePromocodeResponseDto> validarCodigo(
            @RequestParam String code) {
        ValidatePromocodeResponseDto response = promocodeService.validarCodigo(code);
        return ResponseEntity.ok(response);
    }

    /**
     * Aplica un código promocional a un monto y calcula el descuento
     * 
     * @param request DTO con código, orderId y monto original
     * @return ResponseEntity con el descuento calculado y nuevo total
     * 
     * @example
     * POST /api/promocodes/apply
     * Content-Type: application/json
     * {
     *   "code": "SUMMER20",
     *   "orderId": "550e8400-e29b-41d4-a716-446655440001",
     *   "originalAmount": 500000.00
     * }
     * 
     * Response: 200 OK
     * {
     *   "promocodeId": "550e8400-e29b-41d4-a716-446655440000",
     *   "code": "SUMMER20",
     *   "discountType": "PERCENTAGE",
     *   "discountValue": 20.00,
     *   "discountApplied": 100000.00,
     *   "newTotal": 400000.00,
     *   "newTotalInCents": 40000000,
     *   "message": "Descuento aplicado exitosamente"
     * }
     */
    @PostMapping("/apply")
    public ResponseEntity<ApplyPromocodeResponseDto> aplicarCodigo(
            @Valid @RequestBody ApplyPromocodeRequestDto request) {
        ApplyPromocodeResponseDto response = promocodeService.aplicarCodigo(request);
        return ResponseEntity.ok(response);
    }
}

