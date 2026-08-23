/**
 * Server-side translation-layer component of the Reviews feature. Converts {@code Review} JPA
 * entities into the {@code ReviewResponse} DTO shared with the Android client, and centralizes
 * the average-rating arithmetic shared by {@code ReviewServiceImp} (the persisted recipe rating)
 * and {@code RecipeMapper} (the recipe-detail response's recomputed, visible-only rating).
 */
package com.cooksync_server.mappers;

import java.math.BigDecimal;
import java.util.Collection;

import com.cooksync_server.entities.Review;
import com.dtos.response.review.ReviewResponse;

/**
 * Mapper utility class transforming Review entities into ReviewResponse DTOs, and providing the
 * average-rating computation shared across the review and recipe mapping layers.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public final class ReviewMapper {

    private ReviewMapper() {
    }

    /**
     * Converts a Review entity into a ReviewResponse DTO.
     *
     * @param review target Review entity instance
     * @return populated ReviewResponse instance, or {@code null} if {@code review} is {@code null}
     */
    public static ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        String userId = review.getUser() == null ? null : review.getUser().getId();
        String authorName = review.getUser() == null ? null : review.getUser().getFullName();
        String authorAvatarUrl = review.getUser() == null ? null : review.getUser().getAvatarUrl();
        String recipeId = review.getRecipe() == null ? null : review.getRecipe().getId();
        String created = MapperUtils.toIsoStringOrNull(review.getCreatedAt());
        String updated = MapperUtils.toIsoStringOrNull(review.getUpdatedAt());
        return new ReviewResponse(
                review.getId(),
                userId,
                authorName,
                authorAvatarUrl,
                recipeId,
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                created,
                updated
        );
    }

    /**
     * Computes the arithmetic mean of a collection of ratings, shared by every call site that
     * needs to average a recipe's reviews (which reviews are included is the caller's choice).
     *
     * @param ratings the ratings to average
     * @return the average rating, or {@code null} if {@code ratings} is null or empty
     */
    public static Double averageRating(Collection<BigDecimal> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return null;
        }
        return ratings.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
    }
}
