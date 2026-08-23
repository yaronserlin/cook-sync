package com.dtos.request.auth;

/**
 * Request payload for updating a user's public-profile visibility preferences. Submitted from the
 * privacy toggles on the Android client's Settings screen and applied by the server's
 * {@code PUT /api/auth/privacy} endpoint through the user profile service.
 *
 * @param showRecipesPublicly whether the user's published recipes are shown on their public profile
 * @param showFavoritesPublicly whether other users can see which recipes this user has marked as favorites
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
public record PrivacySettingsUpdateRequestDTO(
        boolean showRecipesPublicly,
        boolean showFavoritesPublicly
) {
}
