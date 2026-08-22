package com.dtos.response.admin;

/**
 * Data Transfer Object containing system-wide statistical metrics for the administrator dashboard.
 * Encapsulates total counts of reported reviews, active recipes, total reviews, tags, and users.
 *
 * @param reportedReviews total count of pending reported reviews requiring moderation
 * @param recipes total count of system recipes
 * @param reviews total count of submitted user reviews
 * @param tags total count of unique ingredient and category tags
 * @param users total count of registered user accounts
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record AdminStatsResponse(
        long reportedReviews,
        long recipes,
        long reviews,
        long tags,
        long users
) {
}
