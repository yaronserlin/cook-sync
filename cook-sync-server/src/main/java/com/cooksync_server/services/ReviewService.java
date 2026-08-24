package com.cooksync_server.services;

import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.dtos.request.review.ReportReviewRequestDTO;
import com.dtos.request.review.ReviewRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.review.ReviewResponse;

/**
 * Service interface for managing recipe reviews, rating recomputation, and moderation reports.
 *
 * @author Yaron Serlin
 * @version 1.2
 * @since 02/08/2026
 */
public interface ReviewService {

    /**
     * Retrieves a paginated list of non-hidden reviews for a recipe, ordered by creation date descending.
     *
     * @param recipeId target recipe ID
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse of ReviewResponse DTOs
     * @throws ResourceNotFoundException if no recipe with the given ID exists
     */
    PagedResponse<ReviewResponse> getReviewsForRecipe(String recipeId, int page, int size);

    /**
     * Adds a review to a recipe and recomputes the recipe's aggregate average rating.
     *
     * @param recipeId target recipe ID
     * @param request review creation request DTO
     * @param userEmail authenticated user email address
     * @throws ResourceNotFoundException if the user or recipe cannot be found
     */
    void addReview(String recipeId, ReviewRequestDTO request, String userEmail);

    /**
     * Deletes a review following ownership authorization and recomputes the recipe's average rating.
     *
     * @param reviewId target review ID
     * @param userEmail authenticated user email address
     * @throws ResourceNotFoundException if the review or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the review's author nor an administrator
     */
    void deleteReview(String reviewId, String userEmail);

    /**
     * Flags a review for moderation audit with a specified reason.
     *
     * @param reviewId target review ID
     * @param request moderation report request DTO
     * @param userEmail email address of the reporting user
     * @throws ResourceNotFoundException if the reporting user or review cannot be found
     */
    void reportReview(String reviewId, ReportReviewRequestDTO request, String userEmail);
}
