package com.cooksync_server.exceptions;

import com.dtos.response.ApiResponse;
import com.dtos.response.errors.ApiErrorResponse;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite validating global REST exception handling and response sanitization.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/08/2026
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleResourceNotFoundException_ShouldReturn404Payload() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Recipe", "123");
        ResponseEntity<ApiResponse<ApiErrorResponse>> response = exceptionHandler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        ApiErrorResponse error = (ApiErrorResponse) response.getBody().error();
        assertEquals("RESOURCE_NOT_FOUND", error.errorCode());
    }

    @Test
    void handleInvalidCredentials_ShouldReturn401Payload() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid password provided");
        ResponseEntity<ApiResponse<ApiErrorResponse>> response = exceptionHandler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        ApiErrorResponse error = (ApiErrorResponse) response.getBody().error();
        assertEquals("INVALID_CREDENTIALS", error.errorCode());
    }

    @Test
    void handleUnauthorizedAction_ShouldReturn403Payload() {
        UnauthorizedActionException ex = new UnauthorizedActionException("User is not recipe owner");
        ResponseEntity<ApiResponse<ApiErrorResponse>> response = exceptionHandler.handleUnauthorizedAction(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        ApiErrorResponse error = (ApiErrorResponse) response.getBody().error();
        assertEquals("UNAUTHORIZED_ACTION", error.errorCode());
    }

    @Test
    void handleAccessDenied_ShouldReturn403Payload() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<ApiResponse<ApiErrorResponse>> response = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        ApiErrorResponse error = (ApiErrorResponse) response.getBody().error();
        assertEquals("ACCESS_DENIED", error.errorCode());
    }

    @Test
    void handleGenericException_ShouldSanitize500Message() {
        RuntimeException ex = new RuntimeException("Test unhandled server exception");
        ResponseEntity<ApiResponse<ApiErrorResponse>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        ApiErrorResponse error = (ApiErrorResponse) response.getBody().error();
        assertFalse(error.message().contains("Test unhandled server exception"));
        assertEquals("An unexpected internal server error occurred. Please try again later.", error.message());
    }
}
