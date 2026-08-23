package com.dtos.response.auth;

/**
 * Response payload carrying an authenticated session, returned by the server's login,
 * registration, and token-refresh flows in {@code AuthController} and the auth service. The
 * Android client's {@code AuthRepository} persists the tokens through {@code SessionManager} and
 * uses the accompanying user details to populate the signed-in experience without a further lookup.
 *
 * @param token the bearer JWT access token used to authorize subsequent API requests
 * @param refreshToken the token used to obtain a new access token once it expires
 * @param userId the unique identifier of the authenticated user
 * @param firstName the user's first name
 * @param lastName the user's last name
 * @param isAdmin whether the user holds administrator privileges
 * @param avatarUrl the hosted URL of the user's avatar image, or null if none is set
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
