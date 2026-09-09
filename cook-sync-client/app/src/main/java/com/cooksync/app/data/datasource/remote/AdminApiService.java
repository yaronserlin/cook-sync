package com.cooksync.app.data.datasource.remote;

import com.dtos.request.admin.AdminUserQueryRequestDTO;
import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.tags.TagMergeRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.AdminStatsResponse;
import com.dtos.response.admin.DuplicateTagGroupResponse;
import com.dtos.response.admin.ReportedReviewResponse;
import com.dtos.response.user.UserResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Retrofit contract for the Admin Console's core dashboard: system stats, the user directory,
 * review-report moderation, and duplicate-tag cleanup. Every endpoint here is admin-only.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface AdminApiService {

    /**
     * Fetches system-wide moderation/content statistics for the Admin Console header.
     *
     * @return call yielding the admin dashboard stats
     */
    @GET("api/admin/stats")
    Call<ApiResponse<AdminStatsResponse>> getAdminStats();

    /**
     * Fetches a paginated, searchable, sortable list of every registered user, for the Admin
     * Console's Users tab.
     *
     * @param params {@link AdminUserQueryRequestDTO#toQueryMap()} for the directory's pagination,
     *               search, filter, and sort parameters
     * @return call yielding a paged collection of user summaries
     */
    @GET("api/admin/users")
    Call<ApiResponse<PagedResponse<UserResponse>>> getAdminUsers(@QueryMap Map<String, String> params);

    /**
     * Fetches a paginated page of reviews currently flagged for moderation, for the Admin
     * Console's Reports tab.
     *
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of reported reviews
     */
    @GET("api/admin/reviews/reported")
    Call<ApiResponse<PagedResponse<ReportedReviewResponse>>> getReportedReviews(@QueryMap Map<String, String> params);

    /**
     * Dismisses a review's report(s) without deleting the review itself (the "Keep" action).
     *
     * @param reviewId the ID of the reported review
     * @return call yielding an empty acknowledgement
     */
    @POST("api/admin/reviews/{id}/dismiss")
    Call<ApiResponse<Void>> dismissReport(@Path("id") String reviewId);

    /**
     * Re-enables a previously disabled user account.
     *
     * @param userId the ID of the user to enable
     * @return call yielding an empty acknowledgement
     */
    @PATCH("api/admin/users/{id}/enable")
    Call<ApiResponse<Void>> enableUser(@Path("id") String userId);

    /**
     * Suspends a user account, blocking sign-in (the "ban user" action).
     *
     * @param userId the ID of the user to suspend
     * @return call yielding an empty acknowledgement
     */
    @PATCH("api/admin/users/{id}/suspend")
    Call<ApiResponse<Void>> suspendUser(@Path("id") String userId);

    /**
     * Permanently deletes a user account and everything it owns (recipes, reviews, favorites,
     * notes, media), bypassing the normal 30-day self-service deletion grace period. Refused by
     * the server if the target is the acting admin's own account or another admin account.
     *
     * @param userId the ID of the user to permanently delete
     * @return call yielding an empty acknowledgement
     */
    @DELETE("api/admin/users/{id}")
    Call<ApiResponse<Void>> deleteUser(@Path("id") String userId);

    /**
     * Fetches a paginated page of tags that appear to be duplicates of one another, for the
     * Admin Console's Tags tab.
     *
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of duplicate tag groups
     */
    @GET("api/admin/tags/duplicates")
    Call<ApiResponse<PagedResponse<DuplicateTagGroupResponse>>> getDuplicateTagGroups(@QueryMap Map<String, String> params);

    /**
     * Merges a duplicate tag into a canonical target tag, repointing every recipe that used
     * the source tag and deleting the source tag row.
     *
     * @param request the source/target tag id pair
     * @return call yielding an empty acknowledgement
     */
    @POST("api/admin/tags/merge")
    Call<ApiResponse<Void>> mergeTags(@Body TagMergeRequestDTO request);
}
