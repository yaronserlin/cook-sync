package com.dtos.response.errors;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object standardizing REST API error response structures across the system.
 * Encapsulates HTTP status code, error classification, error message, path URI, and field-level validation errors.
 *
 * @param timestamp ISO timestamp when the error occurred
 * @param status numeric HTTP response status code
 * @param error HTTP status phrase string
 * @param errorCode system-defined specific error classification code
 * @param message descriptive human-readable error summary
 * @param path request URI path that produced the error
 * @param validationErrors list of field-level validation error details, if applicable
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String errorCode,
        String message,
        String path,
        List<ValidationError> validationErrors
) {

    /**
     * Data Transfer Object detailing a specific field validation failure.
     *
     * @param field target object property name that failed validation
     * @param message failure message describing constraint violation
     * @author Yaron Serlin
     * @version 1.0
     * @since 02/08/2026
     */
    public record ValidationError(String field, String message) {
    }
}
