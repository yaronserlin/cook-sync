package com.cooksync_server.services;

import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;

/**
 * Service interface for managing a user's favorite recipe bookmarks.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface FavoriteService {

    /**
     * Adds a recipe to the user's favorite bookmarks, if not already bookmarked.
     *
     * @param recipeId target recipe ID
     * @param userEmail authenticated user email address
     * @throws ResourceNotFoundException if the user or recipe cannot be found
     */
    void addFavorite(String recipeId, String userEmail);

    /**
     * Removes a recipe from the user's favorite bookmarks.
     *
     * @param recipeId target recipe ID
     * @param userEmail authenticated user email address
     * @throws ResourceNotFoundException if the user or recipe cannot be found
     */
    void removeFavorite(String recipeId, String userEmail);

    /**
     * Retrieves a paginated list of recipe previews bookmarked as favorite by the user.
     *
     * @param userEmail authenticated user email address
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse of RecipePreviewResponse DTOs
     * @throws ResourceNotFoundException if no user with the given email exists
     */
    PagedResponse<RecipePreviewResponse> getUserFavorites(String userEmail, int page, int size);

    /**
     * Retrieves the publicly visible favorites of a given user, for that user's public profile
     * page. Returns an empty page if the target user has disabled {@code showFavoritesPublicly},
     * regardless of what they actually have favorited. Never includes the target's personal
     * notes on those recipes, since those stay private regardless of this setting.
     *
     * @param userId target user ID
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse of RecipePreviewResponse DTOs, empty if the user opted out
     * @throws ResourceNotFoundException if no user with the given ID exists
     */
    PagedResponse<RecipePreviewResponse> getPublicFavoritesByUser(String userId, int page, int size);
}
