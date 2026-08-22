package com.cooksync_server.services;

import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.response.ingredient.IngredientResponse;

/**
 * Service interface for CRUD operations on recipe ingredient items.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface IngredientService {

    /**
     * Appends a new ingredient entry to a target recipe following ownership authorization.
     *
     * @param recipeId target recipe ID
     * @param request ingredient creation payload DTO
     * @param userEmail authenticated user email address
     * @return IngredientResponse DTO of the saved ingredient
     */
    IngredientResponse addIngredientToRecipe(String recipeId, IngredientRequestDTO request, String userEmail);

    /**
     * Updates an existing ingredient item's details.
     *
     * @param ingredientId target ingredient ID
     * @param request ingredient update payload DTO
     * @param userEmail authenticated user email address
     * @return IngredientResponse DTO of the updated ingredient
     */
    IngredientResponse updateIngredient(String ingredientId, IngredientRequestDTO request, String userEmail);

    /**
     * Deletes an ingredient item from a recipe following ownership authorization.
     *
     * @param ingredientId target ingredient ID
     * @param userEmail authenticated user email address
     */
    void deleteIngredient(String ingredientId, String userEmail);
}
