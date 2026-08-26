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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.services.ReviewService;
import com.dtos.request.review.ReportReviewRequestDTO;
import com.dtos.request.review.ReviewRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
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

    private final ReviewService reviewService;

    /**
     * Retrieves all review entries for a given recipe ID.
     *
     * @param recipeId target recipe ID
     * @param page page number
     * @param size page size
     * @return response entity containing paged ReviewResponse DTOs
     * @throws ResourceNotFoundException if no recipe with the given ID exists
     */
    @GetMapping("/recipes/{recipeId}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getReviewsForRecipe(
            @PathVariable String recipeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ReviewResponse> reviews = reviewService.getReviewsForRecipe(recipeId, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, reviews, null, "Reviews retrieved successfully"));
    }

    /**
     * Submits a new review and rating for a recipe.
     *
     * @param recipeId target recipe ID
     * @param request review creation request DTO
     * @param authentication active user authentication token
     * @return response entity acknowledging review addition
     * @throws ResourceNotFoundException if the user or recipe cannot be found
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
     * @param reviewId target review ID
     * @param authentication active user authentication token
     * @return response entity acknowledging review deletion
     * @throws ResourceNotFoundException if the review or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the review's author nor an administrator
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
