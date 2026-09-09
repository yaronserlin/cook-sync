package com.cooksync.app.data.datasource.remote;

import com.dtos.request.review.ReportReviewRequestDTO;
import com.dtos.request.review.ReviewRequestDTO;
import com.dtos.response.ApiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit contract for submitting, deleting, and reporting recipe reviews.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface ReviewApiService {

    /**
     * Submits a new rating/review for a recipe.
     *
     * @param recipeId the ID of the recipe being reviewed
     * @param request the review payload (rating, title, optional comment)
     * @return call yielding an empty acknowledgement
     */
    @POST("api/recipes/{recipeId}/reviews")
    Call<ApiResponse<Void>> submitReview(
            @Path("recipeId") String recipeId,
            @Body ReviewRequestDTO request
    );

    /**
     * Deletes a review the current user authored.
     *
     * @param reviewId the ID of the review to delete
     * @return call yielding an empty acknowledgement
     */
    @DELETE("api/reviews/{reviewId}")
    Call<ApiResponse<Void>> deleteReview(@Path("reviewId") String reviewId);

    /**
     * Flags a review for moderator review.
     *
     * @param reviewId the ID of the review being reported
     * @param request the report payload (reason + optional comment)
     * @return call yielding an empty acknowledgement
     */
    @POST("api/reviews/{reviewId}/report")
    Call<ApiResponse<Void>> reportReview(
            @Path("reviewId") String reviewId,
            @Body ReportReviewRequestDTO request
    );
}
