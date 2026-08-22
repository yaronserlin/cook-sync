package com.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for requesting a new JWT access token using a refresh token.
 * Encapsulates the refresh token payload required for authorization session extension.
 *
 * @param refreshToken the active refresh token string, must not be blank
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record TokenRefreshRequestDTO(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
