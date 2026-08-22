package com.cooksync_server.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.services.IReviewService;
import com.dtos.request.review.ReportReviewRequestDTO;
import com.dtos.request.review.ReviewRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.review.ReviewResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller managing user reviews, rating submissions, and moderation reports on recipes.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final IReviewService reviewService;

    /**
     * Retrieves all review entries for a given recipe ID.
     *
     * Complexity:
     * Time: O(R) where R is review count for recipe
     * Space: O(R)
     *
     * @param recipeId target recipe ID
     * @param page page number
     * @param size page size
     * @return response entity containing paged ReviewResponse DTOs
     */
    @GetMapping("/recipes/{recipeId}/reviews")
    public ResponseEntity<ApiResponse<com.dtos.response.PagedResponse<ReviewResponse>>> getReviewsForRecipe(
            @PathVariable String recipeId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        com.dtos.response.PagedResponse<ReviewResponse> reviews = reviewService.getReviewsForRecipe(recipeId, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, reviews, null, "Reviews retrieved successfully"));
    }

    /**
     * Submits a new review and rating for a recipe.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param recipeId target recipe ID
     * @param request review creation request DTO
     * @param authentication active user authentication token
     * @return response entity acknowledging review addition
     */
    @PostMapping("/recipes/{recipeId}/reviews")
    public ResponseEntity<ApiResponse<Void>> addReview(
            @PathVariable String recipeId, 
            @Valid @RequestBody ReviewRequestDTO request, 
            Authentication authentication) {
        String userEmail = authentication.getName();
        reviewService.addReview(recipeId, request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, null, null, "Review added successfully"));
    }

    /**
     * Deletes a review entry.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param reviewId target review ID
     * @param authentication active user authentication token
     * @return response entity acknowledging review deletion
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable String reviewId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        reviewService.deleteReview(reviewId, userEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Review deleted successfully"));
    }

    /**
     * Flags a review for moderation audit with specified report reason.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param reviewId target review ID
     * @param request moderation report request DTO
     * @param authentication active user authentication token
     * @return response entity acknowledging review report
     */
    @PostMapping("/reviews/{reviewId}/report")
    public ResponseEntity<ApiResponse<Void>> reportReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ReportReviewRequestDTO request,
            Authentication authentication) {
        reviewService.reportReview(reviewId, request, authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Review reported to moderators"));
    }
}