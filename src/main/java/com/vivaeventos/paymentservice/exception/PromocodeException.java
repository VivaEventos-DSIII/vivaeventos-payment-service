package com.vivaeventos.paymentservice.exception;

/**
 * Excepción lanzada cuando hay un error con los códigos promocionales
 */
public class PromocodeException extends RuntimeException {
    
    private final String errorCode;

    public PromocodeException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PromocodeException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

