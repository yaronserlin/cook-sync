package com.dtos.response.errors;

import java.time.Instant;

/**
 * Data Transfer Object standardizing REST API error response structures across the system.
 * Encapsulates HTTP status code, error classification, error message, and path URI.
 *
 * @param timestamp ISO timestamp when the error occurred
 * @param status numeric HTTP response status code
 * @param error HTTP status phrase string
 * @param errorCode system-defined specific error classification code
 * @param message descriptive human-readable error summary
 * @param path request URI path that produced the error
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String errorCode,
        String message,
        String path
) {

    /**
     * Builds an {@code ApiErrorResponse} stamped with the current instant, the single
     * construction path used by every error-producing call site (the global exception
     * advisor and the JWT authentication entry point alike).
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param status numeric HTTP response status code
     * @param error HTTP status phrase string
     * @param errorCode system-defined specific error classification code
     * @param message descriptive human-readable error summary
     * @param path request URI path that produced the error
     * @return a new {@code ApiErrorResponse} timestamped with {@link Instant#now()}
     */
    public static ApiErrorResponse of(int status, String error, String errorCode, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, errorCode, message, path);
    }
}
