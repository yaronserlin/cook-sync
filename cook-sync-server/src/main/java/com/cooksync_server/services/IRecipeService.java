package com.cooksync_server.services;

import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;

/**
 * Service interface for recipe catalog browsing, searching, creation, update, and deletion.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface IRecipeService {

    /**
     * Retrieves a paginated slice of public recipes for feed infinite scrolling.
     *
     * @param page page index
     * @param size page size limit
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @return PagedResponse containing RecipePreviewResponse DTOs
     */
    PagedResponse<RecipePreviewResponse> getAllRecipesPaged(int page, int size, String sortBy, String difficulty, Double minRating);

    /**
     * Retrieves the full detail view of a single recipe by ID.
     *
     * @param id target recipe ID
     * @return RecipeResponse DTO
     */
    RecipeResponse getRecipeById(String id);

    /**
     * Executes a unified keyword and faceted attribute search across the recipe catalog.
     *
     * @param keyword unified free-text search string
     * @param author author name filter
     * @param ingredient ingredient name filter
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @param page page index
     * @param size page size limit
     * @return PagedResponse containing RecipePreviewResponse DTOs
     */
    PagedResponse<RecipePreviewResponse> searchRecipes(String keyword, String author, String ingredient, String sortBy, String difficulty, Double minRating, int page, int size);

    /**
     * Retrieves public recipes tagged with a specific tag name.
     *
     * @param tagName target tag label name
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @param page page index
     * @param size page size limit
     * @return PagedResponse containing RecipePreviewResponse DTOs
     */
    PagedResponse<RecipePreviewResponse> findRecipesByTag(String tagName, String sortBy, String difficulty, Double minRating, int page, int size);

    /**
     * Retrieves all recipes authored by the authenticated user.
     *
     * @param userEmail user email address
     * @param page page index
     * @param size page size limit
     * @return PagedResponse containing RecipePreviewResponse DTOs
     */
    PagedResponse<RecipePreviewResponse> getMyRecipes(String userEmail, int page, int size);

    /**
     * Retrieves the publicly visible recipes authored by a given user, for that user's public
     * profile page. Returns an empty page if the target user has disabled
     * {@code showRecipesPublicly}, regardless of what recipes they actually have.
     *
     * @param userId target user ID
     * @param page page index
     * @param size page size limit
     * @return PagedResponse containing RecipePreviewResponse DTOs, empty if the user opted out
     */
    PagedResponse<RecipePreviewResponse> getPublicRecipesByUser(String userId, int page, int size);

    /**
     * Creates a new recipe with nested ingredients, instructions, tags, and images.
     *
     * @param request recipe creation request DTO
     * @param userEmail creator user email address
     * @return created RecipeResponse DTO
     */
    RecipeResponse createRecipe(RecipeCreateRequestDTO request, String userEmail);

    /**
     * Updates an existing recipe's attributes, ingredients, instructions, tags, and images.
     *
     * @param recipeId target recipe ID
     * @param request recipe update request DTO
     * @param userEmail user email address
     * @return updated RecipeResponse DTO
     */
    RecipeResponse updateRecipe(String recipeId, RecipeCreateRequestDTO request, String userEmail);

    /**
     * Updates only a recipe's visibility, without touching its other fields.
     *
     * @param recipeId target recipe ID
     * @param request visibility update request DTO
     * @param userEmail user email address
     * @return updated RecipeResponse DTO
     */
    RecipeResponse updateVisibility(String recipeId, RecipeVisibilityUpdateRequestDTO request, String userEmail);

    /**
     * Deletes a recipe by ID following ownership validation.
     *
     * @param recipeId target recipe ID
     * @param userEmail user email address
     */
    void deleteRecipe(String recipeId, String userEmail);
}
