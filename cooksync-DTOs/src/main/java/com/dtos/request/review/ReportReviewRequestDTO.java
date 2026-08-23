/**
 * Shared DTO-layer component of the Reviews feature. Defines the request payload the Android
 * client's {@code ReportReviewDialog}/{@code RecipeRepository.reportReview} sends and
 * {@code ReviewController.reportReview} validates and consumes on the server.
 */
package com.dtos.request.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Data Transfer Object for submitting moderation reports against user reviews.
 * Encapsulates predefined report categories and optional explanatory user commentary.
 *
 * @param reason the violation classification string, restricted to SPAM, ABUSE, or OFF_TOPIC
 * @param comment optional supplementary notes describing the moderation concern
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record ReportReviewRequestDTO(
        @NotBlank(message = "A report reason is required")
        @Pattern(regexp = "SPAM|ABUSE|OFF_TOPIC", message = "Reason must be SPAM, ABUSE, or OFF_TOPIC")
        String reason,

        String comment
) {
}
