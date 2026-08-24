package com.dtos.response.admin;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing a flagged review queued for administrative moderation.
 * Encapsulates review content, reviewer identity, recipe details, report cause, and submission timestamp.
 *
 * @param id the unique identifier of the report entry
 * @param reviewerName the full display name of the review author
 * @param reviewerId the unique user identifier of the review author
 * @param reviewerAvatarUrl the avatar image URL of the review author, or null if none
 * @param recipeId the unique identifier of the associated recipe
 * @param recipeTitle the title of the associated recipe
 * @param reason the report classification category of the most recent report
 * @param comment the review's own commentary text, for moderator context
 * @param reportComment supplementary explanation provided by the most recent reporter, or null if none was given
 * @param rating the numeric review rating score
 * @param reportedAt ISO formatted timestamp string when the most recent report was lodged
 * @author Yaron Serlin
 * @version 1.2
 * @since 02/08/2026
 */
public record ReportedReviewResponse(
        String id,
        String reviewerName,
        String reviewerId,
        String reviewerAvatarUrl,
        String recipeId,
        String recipeTitle,
        String reason,
        String comment,
        String reportComment,
        BigDecimal rating,
        String reportedAt
) {
}
