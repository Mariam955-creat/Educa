package com.educa.backend.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Format d'erreur homogène renvoyé par l'API (voir docs/02-conception.md §4).
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> fieldErrors) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldViolation> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
