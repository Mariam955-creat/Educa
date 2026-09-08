package com.educa.backend.common.error;

import org.springframework.http.HttpStatus;

/**
 * Exception métier portant un statut HTTP. Interceptée par {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
