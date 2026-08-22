package com.cooksync_server.mappers;

import com.cooksync_server.entities.Review;
import com.dtos.response.review.ReviewResponse;

/**
 * Mapper utility class transforming Review entities into ReviewResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public final class ReviewMapper {

    private ReviewMapper() {
    }

    /**
     * Converts a Review entity into a ReviewResponse DTO.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param review target Review entity instance
     * @return populated ReviewResponse instance or null
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
}
