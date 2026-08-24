package com.cooksync_server.services;

import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.response.ingredient.IngredientResponse;

/**
 * Service interface for CRUD operations on recipe ingredient items.
 *
 * @author Yaron Serlin
 * @version 1.1
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
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if the recipe, acting user, or referenced unit cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the recipe owner nor an administrator
     */
    IngredientResponse addIngredientToRecipe(String recipeId, IngredientRequestDTO request, String userEmail);

    /**
     * Updates an existing ingredient item's details.
     *
     * @param ingredientId target ingredient ID
     * @param request ingredient update payload DTO
     * @param userEmail authenticated user email address
     * @return IngredientResponse DTO of the updated ingredient
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if the ingredient, acting user, or referenced unit cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the ingredient's recipe owner nor an administrator
     */
    IngredientResponse updateIngredient(String ingredientId, IngredientRequestDTO request, String userEmail);

    /**
     * Deletes an ingredient item from a recipe following ownership authorization.
     *
     * @param ingredientId target ingredient ID
     * @param userEmail authenticated user email address
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if the ingredient or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the ingredient's recipe owner nor an administrator
     */
    void deleteIngredient(String ingredientId, String userEmail);
}
