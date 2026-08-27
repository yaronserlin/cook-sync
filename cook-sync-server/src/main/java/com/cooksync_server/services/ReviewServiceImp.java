package com.cooksync_server.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtos.request.review.ReportReviewRequestDTO;
import com.dtos.request.review.ReviewRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.review.ReviewResponse;
import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.Review;
import com.cooksync_server.entities.ReviewReport;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.mappers.ReviewMapper;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.ReviewRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing user reviews, rating recomputations, and moderation report submissions.
 *
 * @author Yaron Serlin
 * @version 1.2
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImp implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final ReviewReportRepository reviewReportRepository;

    /**
     * Retrieves all review entries for a recipe ordered by creation date descending.
     *
     * @param recipeId target recipe ID
     * @param page page number
     * @param size page size
     * @return paged response of ReviewResponse DTOs
     * @throws ResourceNotFoundException if no recipe with the given ID exists
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getReviewsForRecipe(String recipeId, int page, int size) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new ResourceNotFoundException(EntityNames.RECIPE, recipeId);
        }

        Page<Review> reviewPage = reviewRepository.findByRecipeIdAndHiddenFalseOrderByCreatedAtDesc(
                recipeId, PageRequest.of(page, size));

        return PagedResponseMapper.toPagedResponse(reviewPage, ReviewMapper::toResponse);
    }

    /**
     * Adds a review to a recipe and recomputes the recipe's aggregate average rating.
     *
     * @param recipeId target recipe ID
     * @param request review creation request DTO
     * @param userEmail user email address
     * @throws ResourceNotFoundException if the user or recipe cannot be found
     */
    @Transactional
    public void addReview(String recipeId, ReviewRequestDTO request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.RECIPE, recipeId));

        Review review = Review.builder()
                .user(user)
                .recipe(recipe)
                .rating(BigDecimal.valueOf(request.rating()))
                .title(request.title())
                .comment(request.comment())
                .build();

        reviewRepository.save(review);

        recipe.setReviewCount(recipe.getReviewCount() + 1);
        recipe.getReviews().add(review);
        recomputeAverageRating(recipe);
        recipeRepository.save(recipe);
    }

    /**
     * Deletes a review entry following authorization checks, recomputes recipe average rating,
     * and clears any moderation reports filed against it first (a non-nullable foreign key
     * would otherwise block the deletion).
     *
     * @param reviewId target review ID
     * @param userEmail user email address
     * @throws ResourceNotFoundException if the review or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the review's author nor an administrator
     */
    @Transactional
    public void deleteReview(String reviewId, String userEmail) {
        Review review = OwnershipValidator.requireOwnedResource(
                () -> reviewRepository.findById(reviewId), EntityNames.REVIEW, reviewId,
                r -> r.getUser().getId(), userRepository, userEmail,
                "You are not allowed to delete this review.");

        Recipe recipe = review.getRecipe();
        recipe.setReviewCount(Math.max(0, recipe.getReviewCount() - 1));
        recipe.getReviews().removeIf(r -> r.getId().equals(review.getId()));
        recomputeAverageRating(recipe);
        recipeRepository.save(recipe);

        reviewReportRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);
    }

    /**
     * Flags a review for moderation audit with the specified reason, persisting an independent
     * {@link ReviewReport} record per submission so multiple users can report the same review
     * without overwriting one another's reason/comment. The flat {@code reported}/
     * {@code reportReason}/{@code reportedAt} fields on {@link Review} are also refreshed to
     * reflect this latest report, preserving the existing admin moderation console's
     * "currently reported" flag and dashboard count.
     *
     * @param reviewId target review ID
     * @param request moderation report request DTO
     * @param userEmail email address of the reporting user
     * @throws ResourceNotFoundException if the reporting user or review cannot be found
     */
    @Transactional
    public void reportReview(String reviewId, ReportReviewRequestDTO request, String userEmail) {
        User reporter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.REVIEW, reviewId));

        Review.ReportReason reason = Review.ReportReason.valueOf(request.reason().toUpperCase());

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporter(reporter)
                .reason(reason)
                .comment(request.comment())
                .build();
        reviewReportRepository.save(report);

        review.setReported(true);
        review.setReportReason(reason);
        review.setReportedAt(report.getCreatedAt());
        reviewRepository.save(review);
    }

    /**
     * Recomputes and applies a recipe's denormalized average rating from its full review set
     * (including reviews currently hidden by the account-deletion grace-period flow — see
     * {@link ReviewMapper#averageRating} for the visible-only variant used by the recipe-detail
     * response).
     *
     * @param recipe the recipe whose {@code averageRating} is recomputed and set in place
     */
    private void recomputeAverageRating(Recipe recipe) {
        Set<Review> reviews = recipe.getReviews();
        List<BigDecimal> ratings = reviews == null ? List.of()
                : reviews.stream().map(Review::getRating).toList();
        recipe.setAverageRating(ReviewMapper.averageRating(ratings));
    }
}
