package com.cooksync_server.services;

import com.dtos.request.tags.TagMergeRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.AdminStatsResponse;
import com.dtos.response.admin.DuplicateTagGroupResponse;
import com.dtos.response.admin.ReportedReviewResponse;
import com.dtos.response.user.UserResponse;

/**
 * Service interface for administrative moderation, user management, and tag deduplication operations.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface AdminService {

    /**
     * Calculates system-wide aggregate stats for the admin dashboard.
     *
     * @return AdminStatsResponse containing counts of reported reviews, recipes, reviews, tags, and users
     */
    AdminStatsResponse getStats();

    /**
     * Retrieves a paginated, optionally search-filtered and sorted list of registered users.
     *
     * @param page page number index
     * @param size page size limit
     * @param q optional search fragment matched against first name, last name, or email
     * @param enabled optional account status filter (true = active, false = disabled, null = both)
     * @param sortBy field to sort by; must be one of firstName, lastName, email, createdAt
     * @param direction sort direction, "asc" or "desc" (default desc)
     * @return PagedResponse containing UserResponse DTOs
     */
    PagedResponse<UserResponse> getAllUsers(int page, int size, String q, Boolean enabled, String sortBy, String direction);

    /**
     * Retrieves a paginated list of reviews currently flagged as reported.
     *
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse containing ReportedReviewResponse DTOs
     */
    PagedResponse<ReportedReviewResponse> getReportedReviews(int page, int size);

    /**
     * Dismisses the moderation report flag on a specific review.
     *
     * @param reviewId target review ID
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no review with the given ID exists
     */
    void dismissReport(String reviewId);

    /**
     * Suspends a user account, preventing login and hiding their authored recipes and reviews,
     * and revokes their active session so the suspension takes effect immediately rather than
     * after their refresh token would otherwise have expired. Refuses to suspend the acting
     * admin's own account or any other admin account.
     *
     * @param userId target user ID
     * @param actingAdminEmail email of the admin performing the suspension, used for the
     *                          self-suspension guard
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user with the given ID exists
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the target is the acting admin's own account or another admin account
     */
    void suspendUser(String userId, String actingAdminEmail);

    /**
     * Reactivates a previously suspended or deactivated user account.
     *
     * @param userId target user ID
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user with the given ID exists
     */
    void enableUser(String userId);

    /**
     * Permanently deletes a user account and everything it owns (recipes, reviews, favorites,
     * notes, media), bypassing the normal 30-day self-service deletion grace period entirely.
     * Refuses to delete the acting admin's own account or any other admin account.
     *
     * @param userId target user ID
     * @param actingAdminEmail email of the admin performing the deletion, used for the
     *                          self-deletion guard
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user with the given ID exists
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the target is the acting admin's own account or another admin account
     */
    void deleteUserPermanently(String userId, String actingAdminEmail);

    /**
     * Scans the tag catalog for duplicate tag groups based on normalized name formatting.
     *
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse containing DuplicateTagGroupResponse DTOs
     */
    PagedResponse<DuplicateTagGroupResponse> getDuplicateTagGroups(int page, int size);

    /**
     * Merges a duplicate source tag into a canonical target tag and deletes the source tag.
     *
     * @param request tag merge request DTO containing source and target tag IDs
     * @throws IllegalArgumentException if the source and target tag IDs are the same
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if the source or target tag cannot be found
     */
    void mergeTags(TagMergeRequestDTO request);
}
