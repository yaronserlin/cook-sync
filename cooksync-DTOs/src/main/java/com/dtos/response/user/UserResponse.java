package com.dtos.response.user;

/**
 * Data Transfer Object representing a user profile summary in API responses.
 * Encapsulates user identity, name details, email address, role flags, avatar link, timestamps, and account status.
 *
 * @param id unique identifier of the user account
 * @param firstName user's first name
 * @param lastName user's last name
 * @param email user's registered email address
 * @param isAdmin boolean flag indicating administrator authority
 * @param avatarUrl web URL pointing to the user's avatar image, or null if unset
 * @param createdAt ISO formatted account creation timestamp string
 * @param updatedAt ISO formatted profile update timestamp string
 * @param enabled boolean flag indicating whether account is active
 * @param status account status: "ACTIVE", "DEACTIVATED" (self-service), or "SUSPENDED" (admin-imposed)
 * @param city user's self-reported city, shown on their public profile, or null if unset
 * @param bio user's self-authored short biography, or null if unset
 * @param showRecipesPublicly whether the user's published recipes appear on their public profile
 * @param showFavoritesPublicly whether other users can see which recipes this user has favorited
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
