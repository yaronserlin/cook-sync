package com.cooksync_server.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.constants.PaginationDefaults;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
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
     * @param page zero-based page index
     * @param size page size limit
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @return response entity containing PagedResponse of RecipePreviewResponse DTOs
     */
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> getAllRecipesPaged(
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Double minRating) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                recipeService.getAllRecipesPaged(page, size, sortBy, difficulty, minRating),
                null, "Recipes retrieved successfully"));
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
        return ResponseEntity.ok(new ApiResponse<>(true, recipe, null, "Recipe retrieved successfully"));
    }

    /**
     * Executes unified keyword and faceted attribute search across recipe catalog.
     * Supports server-side sorting and filtering via optional query parameters.
     *
     * @param q unified free-text search string
     * @param author author name filter string
     * @param ingredient ingredient name filter string
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @param page zero-based page index
     * @param size page size limit
     * @return response entity containing search result list of RecipePreviewResponse DTOs
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> searchRecipes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String ingredient,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size) {
        PagedResponse<RecipePreviewResponse> recipes = recipeService.searchRecipes(q, author, ingredient, sortBy, difficulty, minRating, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, recipes, null, "Search completed"));
    }

    /**
     * Filters public recipes associated with a specific tag name.
     * Supports server-side sorting and filtering via optional query parameters.
     *
     * @param tagName target tag label name
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @param page zero-based page index
     * @param size page size limit
     * @return response entity containing list of RecipePreviewResponse DTOs
     */
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> getRecipesByTag(
            @PathVariable String tagName,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size) {
        PagedResponse<RecipePreviewResponse> recipes = recipeService.findRecipesByTag(tagName, sortBy, difficulty, minRating, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, recipes, null, "Recipes retrieved by tag"));
    }

    /**
     * Retrieves all recipes authored by the currently authenticated user.
     *
     * @param authentication active user authentication token
     * @param page zero-based page index
     * @param size page size limit
     * @return response entity containing user's RecipePreviewResponse DTOs
     */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> getMyRecipes(
            Authentication authentication,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size) {
        PagedResponse<RecipePreviewResponse> recipes = recipeService.getMyRecipes(authentication.getName(), page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, recipes, null, "Your recipes retrieved successfully"));
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
                .body(new ApiResponse<>(true, createdRecipe, null, "Recipe created successfully"));
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
        return ResponseEntity.ok(new ApiResponse<>(true, updatedRecipe, null, "Recipe updated successfully"));
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
        return ResponseEntity.ok(new ApiResponse<>(true, updatedRecipe, null, "Visibility updated successfully"));
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
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Recipe deleted successfully"));
    }
}
