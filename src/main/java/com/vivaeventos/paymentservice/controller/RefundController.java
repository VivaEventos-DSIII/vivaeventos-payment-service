package com.vivaeventos.paymentservice.controller;

import com.vivaeventos.paymentservice.dto.RefundRequestDto;
import com.vivaeventos.paymentservice.dto.RefundResponseDto;
import com.vivaeventos.paymentservice.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundResponseDto> solicitarDevolucion(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody RefundRequestDto request) {
        UUID customerId = UUID.nameUUIDFromBytes(userEmail.getBytes(StandardCharsets.UTF_8));
        RefundResponseDto response = refundService.registrarDevolucion(request, userEmail, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
