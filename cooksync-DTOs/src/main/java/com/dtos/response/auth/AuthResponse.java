package com.dtos.response.auth;

/**
 * Data Transfer Object containing authentication response payloads returned after login, registration, or token renewal.
 * Encapsulates JWT access token, refresh token, user identity credentials, and role authorization flags.
 *
 * @param token the active bearer JWT access token string
 * @param refreshToken the active refresh token string for token renewal
 * @param userId the unique identifier of the authenticated user
 * @param firstName the user's first name
 * @param lastName the user's last name
 * @param isAdmin boolean flag indicating whether the user possesses administrative privileges
 * @param avatarUrl web URL pointing to the user's avatar image, or null if unset
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record AuthResponse(
        String token,
        String refreshToken,
        String userId,
        String firstName,
        String lastName,
        boolean isAdmin,
        String avatarUrl
) {
}
