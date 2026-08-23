package com.dtos.response.user;

/**
 * Response payload representing another user's public-facing profile, returned by
 * {@code GET /api/users/{id}}. Deliberately narrower than {@link UserResponse}: excludes
 * {@code email}, {@code isAdmin}, {@code enabled}, and {@code status}, none of which are
 * appropriate to disclose to an arbitrary authenticated user viewing someone else's profile.
 *
 * @param id unique identifier of the user account
 * @param firstName the user's first name
 * @param lastName the user's last name
 * @param avatarUrl the hosted URL of the user's avatar image, or null if none is set
 * @param city the user's self-reported city shown on their public profile, or null if unset
 * @param bio the user's self-authored short biography, or null if unset
 * @param showRecipesPublicly whether the user's published recipes are shown on their public profile
 * @param showFavoritesPublicly whether other users can see which recipes this user has marked as favorites
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
public record PublicUserProfileResponse(
        String id,
        String firstName,
        String lastName,
        String avatarUrl,
        String city,
        String bio,
        boolean showRecipesPublicly,
        boolean showFavoritesPublicly
) {
}
