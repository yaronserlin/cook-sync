package com.cooksync_server.config;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.dtos.response.ApiResponse;
import com.dtos.response.errors.ApiErrorResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security AuthenticationEntryPoint handling unauthorized endpoint access attempts.
 * Formats structured JSON ApiErrorResponse payload when a valid JWT token is missing or expired.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Constructs JwtAuthenticationEntryPoint with Jackson ObjectMapper dependency.
     *
     * @param objectMapper Jackson JSON mapper instance
     */
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Commences unauthorized error response write operation.
     *
     * @param request incoming HTTP servlet request
     * @param response outgoing HTTP servlet response
     * @param authException authentication exception details
     * @throws IOException if network writing error occurs
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "INVALID_OR_MISSING_TOKEN",
                "Authentication is required or the provided token is invalid/expired",
                request.getRequestURI(),
                null
        );

        objectMapper.writeValue(response.getWriter(), new ApiResponse<>(false, null, error, null));
    }
}
