package com.vivaeventos.paymentservice.example;

import com.vivaeventos.paymentservice.dto.ApplyPromocodeRequestDto;
import com.vivaeventos.paymentservice.dto.ApplyPromocodeResponseDto;
import com.vivaeventos.paymentservice.dto.OrdenListaParaPagoEvent;
import com.vivaeventos.paymentservice.exception.PromocodeException;
import com.vivaeventos.paymentservice.service.PromocodeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * EJEMPLO DE INTEGRACIÓN: Cómo usar PromoCode en el flujo de pago
 *
 * Este archivo muestra cómo integrar la funcionalidad de códigos promocionales
 * con el flujo de pago existente.
 *
 * NOTA: Este es un archivo de ejemplo REFERENCIALMENTE. No incluirlo en producción.
 */
@Service
public class PaymentServiceIntegrationExample {

    private final PromocodeService promocodeService;

    public PaymentServiceIntegrationExample(PromocodeService promocodeService) {
        this.promocodeService = promocodeService;
    }

    /**
     * Ejemplo 1: Validar un código antes de proceder con el pago
     *
     * Caso de uso: El cliente ingresa un código y queremos validarlo
     * sin afectar el contador de usos
     */
    public boolean validarCodigoPromocional(String code) {
        try {
            var response = promocodeService.validarCodigo(code);
            return response.valid();
        } catch (PromocodeException e) {
            // El código no es válido
            System.err.println("Código inválido: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ejemplo 2: Calcular monto final con descuento antes de crear el pago
     *
     * Caso de uso: El cliente aplica un código e inmediatamente inicia pago
     */
    public Long calcularMontoConDescuento(String code, Long amountInCents, String orderId) {
        // Convertir centavos a unidades para el cálculo
        BigDecimal originalAmount = BigDecimal.valueOf(amountInCents)
                .divide(BigDecimal.valueOf(100));

        try {
            ApplyPromocodeRequestDto request = new ApplyPromocodeRequestDto(
                    code,
                    java.util.UUID.fromString(orderId),
                    originalAmount
            );

            ApplyPromocodeResponseDto response = promocodeService.aplicarCodigo(request);

            // Retornar el monto final en centavos para usar en Wompi
            return response.newTotalInCents();

        } catch (PromocodeException e) {
            System.err.println("Error al aplicar código: " + e.getMessage());
            // Si hay error, retornar el monto original (sin descuento)
            return amountInCents;
        }
    }

    /**
     * Ejemplo 3: Modificar OrdenListaParaPagoEvent para incluir descuento
     *
     * Caso de uso: Antes de pasar el evento a Kafka/PaymentService
     */
    public class OrdenListaParaPagoEventConDescuento extends OrdenListaParaPagoEvent {
        private final String promocode;
        private final ApplyPromocodeResponseDto descuentoAplicado;

        public OrdenListaParaPagoEventConDescuento(
                OrdenListaParaPagoEvent original,
                String promocode,
                ApplyPromocodeResponseDto descuentoAplicado) {
            super(
                    original.orderId(),
                    original.ticketTypeId(),
                    original.customerEmail(),
                    original.customerName(),
                    descuentoAplicado.newTotalInCents(), // <-- USO DEL MONTO CON DESCUENTO
                    original.cardToken(),
                    original.customerPhone()
            );
            this.promocode = promocode;
            this.descuentoAplicado = descuentoAplicado;
        }

        public String getPromocode() {
            return promocode;
        }

        public ApplyPromocodeResponseDto getDescuentoAplicado() {
            return descuentoAplicado;
        }

        @Override
        public String toString() {
            return "OrdenListaParaPagoEventConDescuento{" +
                    "orderId=" + orderId() +
                    ", amountInCents=" + amountInCents() + " (descuento aplicado)" +
                    ", promocode='" + promocode + '\'' +
                    ", descuentoAplicado=" + descuentoAplicado +
                    '}';
        }
    }

    /**
     * Ejemplo 4: Flujo completo: Validar → Aplicar → Pagar
     *
     * Caso de uso: El flujo completo desde que el cliente envía el código
     */
    public FlujoPagoResult procesarPagoConCodigo(
            String code,
            OrdenListaParaPagoEvent ordenOriginal) {

        // PASO 1: Validar que el código existe y es válido
        try {
            var validacion = promocodeService.validarCodigo(code);
            if (!validacion.valid()) {
                return FlujoPagoResult.error("Código no válido: " + validacion.message());
            }
        } catch (PromocodeException e) {
            return FlujoPagoResult.error("Código no encontrado: " + e.getMessage());
        }

        // PASO 2: Aplicar el código (esto incrementa el contador)
        BigDecimal montoOriginal = BigDecimal.valueOf(ordenOriginal.amountInCents())
                .divide(BigDecimal.valueOf(100));

        ApplyPromocodeResponseDto descuento;
        try {
            descuento = promocodeService.aplicarCodigo(
                    new ApplyPromocodeRequestDto(
                            code,
                            ordenOriginal.orderId(),
                            montoOriginal
                    )
            );
        } catch (PromocodeException e) {
            return FlujoPagoResult.error("No se pudo aplicar el código: " + e.getMessage());
        }

        // PASO 3: Crear evento modificado con nuevo monto
        var ordenConDescuento = new OrdenListaParaPagoEventConDescuento(
                ordenOriginal,
                code,
                descuento
        );

        // PASO 4: El PaymentService usaría esta orden con el nuevo monto
        System.out.println("✅ Orden procesada: " + ordenConDescuento);
        System.out.println("   Descuento: " + descuento.discountApplied() + " COP");
        System.out.println("   Nuevo total: " + descuento.newTotal() + " COP");

        return FlujoPagoResult.success(ordenConDescuento, descuento);
    }

    /**
     * Clase auxiliar para retornar resultados
     */
    public static class FlujoPagoResult {
        private final boolean success;
        private final String message;
        private final OrdenListaParaPagoEventConDescuento orden;
        private final ApplyPromocodeResponseDto descuento;

        private FlujoPagoResult(boolean success, String message,
                               OrdenListaParaPagoEventConDescuento orden,
                               ApplyPromocodeResponseDto descuento) {
            this.success = success;
            this.message = message;
            this.orden = orden;
            this.descuento = descuento;
        }

        public static FlujoPagoResult success(OrdenListaParaPagoEventConDescuento orden,
                                             ApplyPromocodeResponseDto descuento) {
            return new FlujoPagoResult(true, "OK", orden, descuento);
        }

        public static FlujoPagoResult error(String message) {
            return new FlujoPagoResult(false, message, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public OrdenListaParaPagoEventConDescuento getOrden() {
            return orden;
        }

        public ApplyPromocodeResponseDto getDescuento() {
            return descuento;
        }
    }
}

/**
 * ============================================================================
 * EJEMPLOS DE USO EN CONTROLLER
 * ============================================================================
 *
 * @RestController
 * @RequestMapping("/api/payment-flow")
 * public class PaymentFlowController {
 *
 *     private final PaymentServiceIntegrationExample example;
 *
 *     @PostMapping("/process-with-promo")
 *     public ResponseEntity<?> procesarPagoConPromo(
 *         @RequestParam String code,
 *         @RequestBody OrdenListaParaPagoEvent orden) {
 *
 *         var result = example.procesarPagoConCodigo(code, orden);
 *
 *         if (!result.isSuccess()) {
 *             return ResponseEntity.badRequest().body(result.getMessage());
 *         }
 *
 *         // Aquí se pasaría a PaymentService con orden actualizada
 *         return ResponseEntity.ok(result.getDescuento());
 *     }
 * }
 *
 * ============================================================================
 * EJEMPLOS DE USO EN TESTS
 * ============================================================================
 *
 * @Test
 * void testFlujoCompletoPagoConCodigo() {
 *     // Crear evento original
 *     var orden = new OrdenListaParaPagoEvent(...);
 *
 *     // Procesardorf con código promocional
 *     var resultado = example.procesarPagoConCodigo("SUMMER20", orden);
 *
 *     // Verificaciones
 *     assertTrue(resultado.isSuccess());
 *     assertEquals("SUMMER20", resultado.getOrden().getPromocode());
 *     assertTrue(resultado.getDescuento().newTotalInCents() < orden.amountInCents());
 * }
 *
 * ============================================================================
 * DIAGRAMA DE FLUJO
 * ============================================================================
 *
 * Cliente
 *    |
 *    | POST /checkout {"code": "SUMMER20", ...}
 *    v
 * PromocodeController.apply()
 *    |
 *    | validarCodigo() → ¿válido?
 *    | ✓ Sí: continuar
 *    | ✗ No: error 400
 *    v
 * PromocodeService.aplicarCodigo()
 *    |
 *    | 1. Validar (existencia, activo, expiración, límite)
 *    | 2. Calcular descuento (PERCENTAGE o FIXED)
 *    | 3. Incrementar usedCount
 *    | 4. Guardar cambios (transacción atómica)
 *    v
 * ApplyPromocodeResponseDto (monto nuevo en centavos)
 *    |
 *    | Crear OrdenListaParaPagoEventConDescuento
 *    | con newTotalInCents
 *    v
 * PaymentService.initiatePago()
 *    |
 *    | Usar amountInCents descuento para crear
 *    | transacción en Wompi
 *    v
 * Wompi
 *    |
 *    | Procesar transacción de MENOR monto
 *    |
 *
 * ============================================================================
 * NOTAS IMPORTANTES
 * ============================================================================
 *
 * 1. El contador de usos se incrementa al APLICAR, no al VALIDAR
 *    → Úsalo cuando estés seguro de que se procesará el pago
 *
 * 2. Los descuentos nunca resultan en totales negativos
 *    → Sistema automáticamente limita al monto original
 *
 * 3. La búsqueda de códigos es case-insensitive
 *    → SUMMER20 = summer20 = Summer20
 *
 * 4. Las transacciones son atómicas
 *    → Si falla el incremento de usedCount, el descuento no se aplica
 *
 * 5. Los errores retornan PromocodeException con errorCode
 *    → GlobalExceptionHandler convierte a ProblemDetail (RFC 7807)
 *
 */

