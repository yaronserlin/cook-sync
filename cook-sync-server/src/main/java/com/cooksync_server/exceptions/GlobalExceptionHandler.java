package com.cooksync_server.exceptions;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dtos.response.ApiResponse;
import com.dtos.response.errors.ApiErrorResponse;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.exceptions.auth.InvalidOtpException;
import com.cooksync_server.exceptions.auth.OtpExpiredException;
import com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.exceptions.auth.UserAlreadyExistsException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global REST exception advisor intercepting exceptions thrown across service and controller tiers.
 * Maps domain runtime exceptions into standardized HTTP ApiResponse payloads. Every mapped
 * exception is logged here at WARN (the single point where all of them are guaranteed to pass
 * through), so call sites do not need their own duplicate pre-throw log statements.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException and responds with HTTP 404 NOT_FOUND.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target exception instance
     * @return response entity containing formatted error payload
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    /**
     * Handles ResourceAlreadyExistsException and responds with HTTP 409 CONFLICT.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target exception instance
     * @return response entity containing formatted error payload
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
        log.warn("Resource already exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", "RESOURCE_ALREADY_EXISTS", ex.getMessage());
    }

    /**
     * Handles UnauthorizedActionException and responds with HTTP 403 FORBIDDEN.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target exception instance
     * @return response entity containing formatted error payload
     */
    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleUnauthorizedAction(UnauthorizedActionException ex) {
        log.warn("Unauthorized action: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", "UNAUTHORIZED_ACTION", ex.getMessage());
    }

    /**
     * Handles InvalidCredentialsException and responds with HTTP 401 UNAUTHORIZED.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target exception instance
     * @return response entity containing formatted error payload
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Invalid credentials: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "INVALID_CREDENTIALS", ex.getMessage());
    }

    /**
     * Handles UserAlreadyExistsException and responds with HTTP 409 CONFLICT.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target exception instance
     * @return response entity containing formatted error payload
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", "USER_ALREADY_EXISTS", ex.getMessage());
    }

    /**
     * Handles InvalidOtpException and responds with HTTP 400 BAD_REQUEST.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target exception instance
     * @return response entity containing formatted error payload
     */
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleInvalidOtp(InvalidOtpException ex) {
        log.warn("Invalid OTP submission: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "INVALID_OTP", ex.getMessage());
    }

    /**
     * Handles OtpExpiredException and responds with HTTP 400 BAD_REQUEST.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target exception instance
     * @return response entity containing formatted error payload
     */
    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleOtpExpired(OtpExpiredException ex) {
        log.warn("OTP expired: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "OTP_EXPIRED", ex.getMessage());
    }

    /**
     * Handles TooManyOtpAttemptsException and responds with HTTP 429 TOO_MANY_REQUESTS.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target exception instance
     * @return response entity containing formatted error payload
     */
    @ExceptionHandler(TooManyOtpAttemptsException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleTooManyOtpAttempts(TooManyOtpAttemptsException ex) {
        log.warn("Too many OTP attempts: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "TOO_MANY_OTP_ATTEMPTS", ex.getMessage());
    }

    /**
     * Handles payload validation failures from MethodArgumentNotValidException and responds with HTTP 400.
     *
     * Complexity:
     * Time: O(V) where V is total count of validation field errors
     * Space: O(V)
     *
     * @param ex target validation exception
     * @return response entity with validation errors list
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ApiErrorResponse>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse> errors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach(error -> errors.add(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", "VALIDATION_ERROR", error.getDefaultMessage(), "")));

        log.warn("Request validation failed with {} error(s)", errors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errors, null));
    }

    /**
     * Handles database constraint and integrity violations and responds with HTTP 409 CONFLICT.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex target data integrity exception
     * @return response entity with conflict error payload
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Database data integrity violation: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", "DATA_INTEGRITY_VIOLATION", "Database constraint or duplicate entry violation");
    }

    /**
     * Handles malformed request JSON bodies and responds with HTTP 400 BAD_REQUEST.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex message parsing exception
     * @return response entity with bad request payload
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed HTTP message payload: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "MALFORMED_JSON_PAYLOAD", "Malformed request payload format");
    }

    /**
     * Handles AccessDeniedException and responds with HTTP 403 FORBIDDEN.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex access denied exception instance
     * @return response entity containing forbidden error payload
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", "ACCESS_DENIED", "You do not have permission to perform this action");
    }

    /**
     * Fallback exception handler catching uncaught exceptions and returning HTTP 500.
     * Sanitizes response message to avoid leaking internal trace or implementation details.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param ex unhandled exception instance
     * @return response entity containing generic error payload
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiErrorResponse>> handleGenericException(Exception ex) {
        log.error("Unhandled exception reached GlobalExceptionHandler", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "INTERNAL_SERVER_ERROR", "An unexpected internal server error occurred. Please try again later.");
    }

    /**
     * Builds the standardized error {@link ResponseEntity} shared by every single-error handler
     * in this class, wrapping a freshly built {@link ApiErrorResponse} in an {@link ApiResponse}
     * and setting it as the HTTP response body at the given status.
     *
     * @param status HTTP status to respond with
     * @param error short human-readable status label (e.g. {@code "Bad Request"})
     * @param errorCode machine-readable error code identifying the failure category
     * @param message user-facing description of what went wrong
     * @return response entity carrying the formatted error payload at {@code status}
     */
    private ResponseEntity<ApiResponse<ApiErrorResponse>> buildErrorResponse(
            HttpStatus status, String error, String errorCode, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ApiErrorResponse.of(status.value(), error, errorCode, message, ""), null));
    }
}
