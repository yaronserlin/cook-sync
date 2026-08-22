package com.dtos.response.review;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing a user review entry in API responses.
 * Encapsulates unique review identifier, author info, rating score, review title, commentary, and timestamps.
 *
 * @param id unique identifier of the review record
 * @param userId unique identifier of the review author
 * @param authorName display name of the review author
 * @param authorAvatarUrl avatar image URL of the review author
 * @param recipeId unique identifier of the reviewed recipe
 * @param rating numeric rating score given by the author
 * @param title headline summary title of the review
 * @param comment detailed text commentary from the author
 * @param createdAt ISO formatted creation timestamp string
 * @param updatedAt ISO formatted last update timestamp string
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record ReviewResponse(
        String id,
        String userId,
        String authorName,
        String authorAvatarUrl,
        String recipeId,
        BigDecimal rating,
        String title,
        String comment,
        String createdAt,
        String updatedAt
) {
}
