package com.cooksync_server.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.ingredient.IngredientResponse;
import com.cooksync_server.services.IngredientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller providing granular management endpoints for recipe ingredient components —
 * appending, editing, and removing individual ingredient rows on an existing recipe, each
 * referencing a {@code Unit} by ID. Distinct from {@link RecipeController}'s nested
 * ingredient-list handling at whole-recipe creation/update time, which serves the same
 * {@code IngredientResponse} DTO shape but goes through {@code RecipeServiceImp} instead.
 * Access is ownership-based rather than role-based: every endpoint here requires the caller to
 * either own the ingredient's parent recipe or hold admin privileges, enforced via
 * {@link com.cooksync_server.services.OwnershipValidator} inside the service layer rather than
 * {@code @PreAuthorize}.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    /**
     * Appends a new ingredient entry to a specific recipe.
     *
     * @param recipeId target recipe ID
     * @param request ingredient creation payload DTO
     * @param authentication active user authentication token
     * @return response entity containing created IngredientResponse DTO
     */
    @PostMapping("/recipes/{recipeId}/ingredients")
    public ResponseEntity<ApiResponse<IngredientResponse>> addIngredient(
            @PathVariable String recipeId,
            @Valid @RequestBody IngredientRequestDTO request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        IngredientResponse response = ingredientService.addIngredientToRecipe(recipeId, request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, response, null, "Ingredient added successfully"));
    }

    /**
     * Updates an existing ingredient entry.
     *
     * @param ingredientId target ingredient ID
     * @param request ingredient update payload DTO
     * @param authentication active user authentication token
     * @return response entity containing updated IngredientResponse DTO
     */
    @PutMapping("/ingredients/{ingredientId}")
    public ResponseEntity<ApiResponse<IngredientResponse>> updateIngredient(
            @PathVariable String ingredientId,
            @Valid @RequestBody IngredientRequestDTO request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        IngredientResponse response = ingredientService.updateIngredient(ingredientId, request, userEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Ingredient updated successfully"));
    }

    /**
     * Deletes an ingredient entry from a recipe.
     *
     * @param ingredientId target ingredient ID
     * @param authentication active user authentication token
     * @return response entity acknowledging ingredient deletion
     */
    @DeleteMapping("/ingredients/{ingredientId}")
    public ResponseEntity<ApiResponse<Void>> deleteIngredient(
            @PathVariable String ingredientId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        ingredientService.deleteIngredient(ingredientId, userEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Ingredient deleted successfully"));
    }
}