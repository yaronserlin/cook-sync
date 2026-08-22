package com.cooksync_server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.services.IFavoriteService;
import com.cooksync_server.services.IRecipeService;
import com.cooksync_server.services.IUserProfileService;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.user.UserResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller providing public user profile retrieval operations.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 10/08/2026
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserProfileService userProfileService;
    private final IRecipeService recipeService;
    private final IFavoriteService favoriteService;

    /**
     * Fetches public user profile information by user ID.
     *
     * @param id target user ID
     * @return response entity containing UserResponse DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable String id) {
        log.debug("Fetching public profile for user ID: {}", id);
        UserResponse response = userProfileService.getUserProfileById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "User profile retrieved successfully"));
    }

    /**
     * Fetches a page of a user's publicly visible recipes, for their public profile page. Empty
     * if the target user has {@code showRecipesPublicly} disabled.
     *
     * @param id target user ID
     * @param page zero-based page index
     * @param size page size limit
     * @return response entity containing PagedResponse of RecipePreviewResponse DTOs
     */
    @GetMapping("/{id}/recipes")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> getUserPublicRecipes(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching public recipes for user ID: {}", id);
        PagedResponse<RecipePreviewResponse> response = recipeService.getPublicRecipesByUser(id, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "User's public recipes retrieved successfully"));
    }

    /**
     * Fetches a page of a user's publicly visible favorites, for their public profile page.
     * Empty if the target user has {@code showFavoritesPublicly} disabled.
     *
     * @param id target user ID
     * @param page zero-based page index
     * @param size page size limit
     * @return response entity containing PagedResponse of RecipePreviewResponse DTOs
     */
    @GetMapping("/{id}/favorites")
    public ResponseEntity<ApiResponse<PagedResponse<RecipePreviewResponse>>> getUserPublicFavorites(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching public favorites for user ID: {}", id);
        PagedResponse<RecipePreviewResponse> response = favoriteService.getPublicFavoritesByUser(id, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "User's public favorites retrieved successfully"));
    }
}
