package com.cooksync_server.mappers;

import com.cooksync_server.entities.Review;
import com.cooksync_server.entities.ReviewReport;
import com.dtos.response.admin.ReportedReviewResponse;

/**
 * Mapper utility class transforming administrative entities into response DTO objects.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public final class AdminMapper {

    private AdminMapper() {
    }

    /**
     * Converts a Review entity into a ReportedReviewResponse DTO for administrative moderation console.
     *
     * @param review target Review entity instance
     * @param latestReport the most recently submitted {@link ReviewReport} for this review, or
     *                     null if it has never been reported (or the report history predates
     *                     per-report tracking)
     * @return populated ReportedReviewResponse DTO or null if entity is null
     */
    public static ReportedReviewResponse toReportedReviewResponse(Review review, ReviewReport latestReport) {
        if (review == null) {
            return null;
        }
        String reviewerName = review.getUser() == null ? null : review.getUser().getFullName();
        String reviewerId = review.getUser() == null ? null : review.getUser().getId();
        String reviewerAvatarUrl = review.getUser() == null ? null : review.getUser().getAvatarUrl();
        String recipeId = review.getRecipe() == null ? null : review.getRecipe().getId();
        String recipeTitle = review.getRecipe() == null ? null : review.getRecipe().getTitle();
        String reason = review.getReportReason() == null ? null : review.getReportReason().name();
        String reportComment = latestReport == null ? null : latestReport.getComment();
        String reportedAt = MapperUtils.toIsoStringOrNull(review.getReportedAt());

        return new ReportedReviewResponse(
                review.getId(),
                reviewerName,
                reviewerId,
                reviewerAvatarUrl,
                recipeId,
                recipeTitle,
                reason,
                review.getComment(),
                reportComment,
                review.getRating(),
                reportedAt
        );
    }
}
