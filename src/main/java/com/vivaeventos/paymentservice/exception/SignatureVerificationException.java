package com.vivaeventos.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SignatureVerificationException extends RuntimeException {
    public SignatureVerificationException(String message) {
        super(message);
    }
}
