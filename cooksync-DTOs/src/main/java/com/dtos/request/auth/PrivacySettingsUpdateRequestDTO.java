package com.dtos.request.auth;

/**
 * Data Transfer Object for updating a user's public-profile privacy preferences.
 * Controls whether the user's recipes and favorites are visible to other users.
 *
 * @param showRecipesPublicly whether the user's published recipes appear on their public profile
 * @param showFavoritesPublicly whether other users can see which recipes this user has favorited
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
public record PrivacySettingsUpdateRequestDTO(
        boolean showRecipesPublicly,
        boolean showFavoritesPublicly
) {
}
