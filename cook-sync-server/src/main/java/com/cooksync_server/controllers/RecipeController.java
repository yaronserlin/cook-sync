package com.cooksync_server.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeFeedRequestDTO;
import com.dtos.request.recipe.RecipeSearchRequestDTO;
import com.dtos.request.recipe.RecipeTagFilterRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.cooksync_server.services.RecipeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller managing recipe catalog browsing, searching, creation, update, and deletion endpoints.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * Retrieves a paginated slice of public recipes for feed infinite scrolling.
     * Supports server-side sorting and filtering via optional query parameters.
     *
     * @param request the feed's pagination, sort, and facet-filter parameters
     * @return response entity containing PagedResponse of RecipePreviewResponse DTOs
     */
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> getAllRecipesPaged(@ModelAttribute RecipeFeedRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.getAllRecipesPaged(request), "Recipes retrieved successfully"));
    }

    /**
     * Retrieves full detail view of a single recipe by ID.
     *
     * @param id target recipe unique identifier
     * @return response entity containing full RecipeResponse DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeResponse>> getRecipeById(@PathVariable String id) {
        RecipeResponse recipe = recipeService.getRecipeById(id);
        return ResponseEntity.ok(ApiResponse.success(recipe, "Recipe retrieved successfully"));
    }

    /**
     * Executes unified keyword and faceted attribute search across recipe catalog.
     * Supports server-side sorting and filtering via optional query parameters.
     *
     * @param request the search's keyword/facet, pagination, and sort parameters
     * @return response entity containing search result list of RecipePreviewResponse DTOs
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> searchRecipes(@ModelAttribute RecipeSearchRequestDTO request) {
        PagedResponse<RecipePreviewResponse> recipes = recipeService.searchRecipes(request);
        return ResponseEntity.ok(ApiResponse.success(recipes, "Search completed"));
    }

    /**
     * Filters public recipes associated with a specific tag name.
     * Supports server-side sorting and filtering via optional query parameters.
     *
     * @param tagName target tag label name
     * @param request the browse's pagination, sort, and facet-filter parameters
     * @return response entity containing list of RecipePreviewResponse DTOs
     */
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> getRecipesByTag(
            @PathVariable String tagName,
            @ModelAttribute RecipeTagFilterRequestDTO request) {
        PagedResponse<RecipePreviewResponse> recipes = recipeService.findRecipesByTag(tagName, request);
        return ResponseEntity.ok(ApiResponse.success(recipes, "Recipes retrieved by tag"));
    }

    /**
     * Retrieves all recipes authored by the currently authenticated user.
     *
     * @param authentication active user authentication token
     * @param request pagination parameters
     * @return response entity containing user's RecipePreviewResponse DTOs
     */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> getMyRecipes(
            Authentication authentication,
            @ModelAttribute PageRequestDTO request) {
        PagedResponse<RecipePreviewResponse> recipes = recipeService.getMyRecipes(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(recipes, "Your recipes retrieved successfully"));
    }

    /**
     * Creates a new recipe entry in the system.
     *
     * @param request recipe creation payload DTO
     * @param authentication active user authentication token
     * @return response entity containing created RecipeResponse DTO
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeResponse>> createRecipe(
            @Valid @RequestBody RecipeCreateRequestDTO request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        RecipeResponse createdRecipe = recipeService.createRecipe(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdRecipe, "Recipe created successfully"));
    }

    /**
     * Updates an existing recipe entry.
     *
     * @param id target recipe unique identifier
     * @param request recipe update payload DTO
     * @param authentication active user authentication token
     * @return response entity containing updated RecipeResponse DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeResponse>> updateRecipe(
            @PathVariable String id,
            @Valid @RequestBody RecipeCreateRequestDTO request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        RecipeResponse updatedRecipe = recipeService.updateRecipe(id, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success(updatedRecipe, "Recipe updated successfully"));
    }

    /**
     * Updates only a recipe's visibility (PUBLIC/PRIVATE), without resubmitting the rest of the recipe.
     *
     * @param id target recipe unique identifier
     * @param request visibility update payload DTO
     * @param authentication active user authentication token
     * @return response entity containing updated RecipeResponse DTO
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse<RecipeResponse>> updateVisibility(
            @PathVariable String id,
            @Valid @RequestBody RecipeVisibilityUpdateRequestDTO request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        RecipeResponse updatedRecipe = recipeService.updateVisibility(id, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success(updatedRecipe, "Visibility updated successfully"));
    }

    /**
     * Deletes a recipe by unique ID.
     *
     * @param id target recipe unique identifier
     * @param authentication active user authentication token
     * @return response entity acknowledging recipe deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipe(
            @PathVariable String id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        recipeService.deleteRecipe(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success(null, "Recipe deleted successfully"));
    }
}
