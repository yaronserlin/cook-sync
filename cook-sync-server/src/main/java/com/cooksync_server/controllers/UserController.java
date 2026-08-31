package com.cooksync_server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.constants.PaginationDefaults;
import com.cooksync_server.services.FavoriteService;
import com.cooksync_server.services.RecipeService;
import com.cooksync_server.services.UserProfileService;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.user.PublicUserProfileResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Server-layer REST controller providing public user profile retrieval operations: another
 * user's public profile summary and their public recipes/favorites, both gated by that user's
 * own privacy preferences. Consumed on the Android client by {@code UserProfileActivity} via
 * {@code UserProfileViewModel}. Returns {@code PublicUserProfileResponse} — a narrower DTO than
 * {@code UserResponse} — since these endpoints are reachable by any authenticated user, not just
 * the profile's owner.
 *
 * @author Yaron Serlin
 * @version 1.2
 * @since 10/08/2026
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final RecipeService recipeService;
    private final FavoriteService favoriteService;

    /**
     * Fetches public user profile information by user ID, deliberately excluding fields
     * (email, admin status, account status) not appropriate to disclose to another user.
     *
     * @param id target user ID
     * @return response entity containing a PublicUserProfileResponse DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicUserProfileResponse>> getUserProfile(@PathVariable String id) {
        log.debug("Fetching public profile for user ID: {}", id);
        PublicUserProfileResponse response = userProfileService.getUserProfileById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User profile retrieved successfully"));
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
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size) {
        log.debug("Fetching public recipes for user ID: {}", id);
        PagedResponse<RecipePreviewResponse> response = recipeService.getPublicRecipesByUser(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "User's public recipes retrieved successfully"));
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
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size) {
        log.debug("Fetching public favorites for user ID: {}", id);
        PagedResponse<RecipePreviewResponse> response = favoriteService.getPublicFavoritesByUser(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "User's public favorites retrieved successfully"));
    }
}
