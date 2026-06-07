package com.vivaeventos.paymentservice.controller;

import com.vivaeventos.paymentservice.dto.RefundRequestDto;
import com.vivaeventos.paymentservice.dto.RefundResponseDto;
import com.vivaeventos.paymentservice.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundResponseDto> solicitarDevolucion(@RequestBody RefundRequestDto request) {
        RefundResponseDto response = refundService.registrarDevolucion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
