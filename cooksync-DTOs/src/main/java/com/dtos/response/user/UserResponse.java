package com.dtos.response.user;

/**
 * Response payload representing a full user profile snapshot, returned by endpoints such as
 * {@code GET /api/auth/me} and by the account/admin lookup flows of the user profile and admin
 * services. Consumed on the Android client by the profile and settings screens to render the
 * signed-in user's identity, avatar, and privacy state.
 *
 * @param id unique identifier of the user account
 * @param firstName the user's first name
 * @param lastName the user's last name
 * @param email the user's registered email address
 * @param isAdmin whether the account holds administrator privileges
 * @param avatarUrl the hosted URL of the user's avatar image, or null if none is set
 * @param createdAt the account creation timestamp, formatted as an ISO-8601 string
 * @param updatedAt the timestamp of the account's most recent update, formatted as an ISO-8601 string
 * @param enabled whether the account is currently active and able to authenticate
 * @param status the account's lifecycle state: {@code "ACTIVE"}, {@code "DEACTIVATED"} (self-service), or {@code "SUSPENDED"} (admin-imposed)
 * @param city the user's self-reported city shown on their public profile, or null if unset
 * @param bio the user's self-authored short biography, or null if unset
 * @param showRecipesPublicly whether the user's published recipes are shown on their public profile
 * @param showFavoritesPublicly whether other users can see which recipes this user has marked as favorites
 * @author Yaron Serlin
 * @version 1.2
 * @since 02/08/2026
 */
public record UserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        boolean isAdmin,
        String avatarUrl,
        String createdAt,
        String updatedAt,
        boolean enabled,
        String status,
        String city,
        String bio,
        boolean showRecipesPublicly,
        boolean showFavoritesPublicly
) {
}
