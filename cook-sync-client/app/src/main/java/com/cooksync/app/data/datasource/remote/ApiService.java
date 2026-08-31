package com.cooksync.app.data.datasource.remote;

import com.dtos.request.auth.AvatarUpdateRequestDTO;
import com.dtos.request.auth.ChangePasswordRequestDTO;
import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.ForgotPasswordRequestDTO;
import com.dtos.request.auth.LoginRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.request.auth.RegisterRequestDTO;
import com.dtos.request.auth.ResendRegistrationOtpRequestDTO;
import com.dtos.request.auth.ResetPasswordRequestDTO;
import com.dtos.request.auth.TokenRefreshRequestDTO;
import com.dtos.request.auth.VerifyEmailChangeOtpRequestDTO;
import com.dtos.request.auth.VerifyRegistrationOtpRequestDTO;
import com.dtos.request.note.NoteRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.dtos.request.review.ReportReviewRequestDTO;
import com.dtos.request.review.ReviewRequestDTO;
import com.dtos.request.tags.TagMergeRequestDTO;
import com.dtos.request.tags.TagRequestDTO;
import com.dtos.request.unit.UnitRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.AdminStatsResponse;
import com.dtos.response.admin.DuplicateTagGroupResponse;
import com.dtos.response.admin.ReportedReviewResponse;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.auth.PendingRegistrationResponse;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import com.dtos.response.note.NoteResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.tags.TagResponse;
import com.dtos.response.unit.UnitResponse;
import com.dtos.response.user.PublicUserProfileResponse;
import com.dtos.response.user.UserResponse;

import com.cooksync.app.util.constants.ApiEndpoints;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit contract for every REST endpoint this module (core networking + authentication)
 * depends on. Endpoint paths and payload shapes mirror {@code AuthController} on
 * cook-sync-server exactly, since both sides share the same DTOs from the
 * {@code cooksync-DTOs} artifact. Additional feature areas (recipes, reviews, search, ...)
 * will extend this interface in later modules.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface ApiService {

    /**
     * Initiates registration for a new account. No session is created yet — a one-time
     * verification code is emailed to the given address, and the registration is only
     * completed by calling {@link #verifyRegistrationOtp}.
     *
     * @param request registration payload
     * @return call yielding an acknowledgement of the pending registration
     */
    @POST(ApiEndpoints.REGISTER)
    Call<ApiResponse<PendingRegistrationResponse>> register(@Body RegisterRequestDTO request);

    /**
     * Completes registration by submitting the OTP code emailed for a pending registration.
     *
     * @param request OTP verification payload
     * @return call yielding the newly created session
     */
    @POST(ApiEndpoints.VERIFY_REGISTRATION_OTP)
    Call<ApiResponse<AuthResponse>> verifyRegistrationOtp(@Body VerifyRegistrationOtpRequestDTO request);

    /**
     * Regenerates and re-emails a fresh OTP code for an existing pending registration.
     *
     * @param request resend request payload
     * @return call yielding an acknowledgement of the newly issued OTP
     */
    @POST(ApiEndpoints.RESEND_REGISTRATION_OTP)
    Call<ApiResponse<PendingRegistrationResponse>> resendRegistrationOtp(@Body ResendRegistrationOtpRequestDTO request);

    /**
     * Authenticates an existing user with email and password.
     *
     * @param request login credentials payload
     * @return call yielding the authenticated session
     */
    @POST(ApiEndpoints.LOGIN)
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequestDTO request);

    /**
     * Exchanges a refresh token for a new access/refresh token pair. Invoked exclusively
     * by {@link TokenAuthenticator} in response to a 401 on some other request.
     *
     * @param request refresh token payload
     * @return call yielding the renewed session
     */
    @POST(ApiEndpoints.REFRESH_TOKEN)
    Call<ApiResponse<AuthResponse>> refreshToken(@Body TokenRefreshRequestDTO request);

    /**
     * Validates the current access token and returns the associated user profile.
     *
     * @return call yielding the current session's profile
     */
    @GET("api/auth/validate-token")
    Call<ApiResponse<AuthResponse>> validateToken();

    /**
     * Fetches the authenticated user's full profile, including fields not carried by
     * {@link AuthResponse} (city, bio, privacy preferences). Used to pre-fill the Account
     * Details screen.
     *
     * @return call yielding the current user's full profile
     */
    @GET("api/auth/me")
    Call<ApiResponse<UserResponse>> getCurrentUser();

    /**
     * Fetches a specific user's public profile by ID.
     *
     * @param id target user ID
     * @return call yielding the user's public profile
     */
    @GET("api/users/{id}")
    Call<ApiResponse<PublicUserProfileResponse>> getUserProfile(@Path("id") String id);

    /**
     * Fetches a page of a user's publicly visible recipes, for their public profile page. Empty
     * if the target user has disabled {@code showRecipesPublicly}.
     *
     * @param id target user ID
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of the user's public recipes
     */
    @GET("api/users/{id}/recipes")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getPublicUserRecipes(
            @Path("id") String id,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Fetches a page of a user's publicly visible favorites, for their public profile page.
     * Empty if the target user has disabled {@code showFavoritesPublicly}.
     *
     * @param id target user ID
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of the user's public favorites
     */
    @GET("api/users/{id}/favorites")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getPublicUserFavorites(
            @Path("id") String id,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Invalidates the current refresh token session on the server.
     *
     * @return call yielding an empty acknowledgement
     */
    @POST("api/auth/logout")
    Call<ApiResponse<Void>> logout();

    /**
     * Updates the authenticated user's avatar URL.
     *
     * @param request avatar update payload
     * @return call yielding an empty acknowledgement
     */
    @PUT("api/auth/avatar")
    Call<ApiResponse<Void>> updateAvatar(@Body AvatarUpdateRequestDTO request);

    /**
     * Updates the authenticated user's first/last name.
     *
     * @param request profile update payload
     * @return call yielding an empty acknowledgement
     */
    @PUT("api/auth/profile")
    Call<ApiResponse<Void>> updateProfile(@Body ProfileUpdateRequestDTO request);

    /**
     * Changes the authenticated user's password.
     *
     * @param request password change payload
     * @return call yielding an empty acknowledgement
     */
    @PUT("api/auth/password")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequestDTO request);

    /**
     * Begins changing the authenticated user's email address: verifies the current password and
     * emails a one-time verification code to the requested new address. Also serves as the
     * "resend code" action when called again for the same pending change.
     *
     * @param request email update payload
     * @return call yielding an empty acknowledgement
     */
    @PUT("api/auth/email")
    Call<ApiResponse<Void>> updateEmail(@Body EmailUpdateRequestDTO request);

    /**
     * Completes an email-address change by submitting the OTP code emailed to the pending new
     * address, re-issuing tokens for the new identity.
     *
     * @param request OTP verification payload
     * @return call yielding the renewed session
     */
    @POST("api/auth/email/verify-otp")
    Call<ApiResponse<AuthResponse>> verifyEmailChangeOtp(@Body VerifyEmailChangeOtpRequestDTO request);

    /**
     * Updates the authenticated user's public-profile privacy preferences.
     *
     * @param request privacy settings update payload
     * @return call yielding an empty acknowledgement
     */
    @PUT("api/auth/privacy")
    Call<ApiResponse<Void>> updatePrivacySettings(@Body PrivacySettingsUpdateRequestDTO request);

    /**
     * Starts the 30-day self-service account-deletion grace period for the authenticated user.
     * The account is restored automatically if the user logs back in before the grace period
     * lapses; otherwise it is permanently purged by a server-side scheduled job.
     *
     * @param request delete-account payload carrying the current password for verification
     * @return call yielding an empty acknowledgement
     */
    @HTTP(method = "DELETE", path = "api/auth/account", hasBody = true)
    Call<ApiResponse<Void>> requestAccountDeletion(@Body DeleteAccountRequestDTO request);

    /**
     * Requests a password-reset email for the given account, if it exists.
     *
     * @param request forgot-password payload
     * @return call yielding an empty acknowledgement
     */
    @POST("api/auth/forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body ForgotPasswordRequestDTO request);

    /**
     * Completes a password reset using a token issued via {@link #forgotPassword}.
     *
     * @param request reset-password payload
     * @return call yielding an empty acknowledgement
     */
    @POST("api/auth/reset-password")
    Call<ApiResponse<Void>> resetPassword(@Body ResetPasswordRequestDTO request);

    /**
     * Fetches a short-lived signed payload the client uses to upload media directly to
     * Cloudinary, bypassing the application server for the binary transfer itself.
     *
     * @return call yielding Cloudinary upload credentials
     */
    @GET("api/cloudinary/signature")
    Call<ApiResponse<CloudinarySignatureResponse>> getMediaSignature(
            @Query("folder") String folder,
            @Query("publicId") String publicId
    );

    /**
     * Fetches the environment-specific root Cloudinary folder (e.g. {@code "cooksync-dev"}
     * locally, {@code "CookSyncApp"} in production) so upload folder paths can be built
     * without hardcoding an environment-specific value on the client.
     *
     * @return call yielding the configured base folder name
     */
    @GET("api/cloudinary/base-folder")
    Call<ApiResponse<String>> getCloudinaryBaseFolder();


    /**
     * Fetches a paginated list of public recipe previews for the home feed.
     *
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of recipe previews
     */
    @GET("api/recipes/paged")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getPublicFeed(
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Searches for public recipes matching a text query, author, or ingredient.
     *
     * @param query search text
     * @param author optional author name filter
     * @param ingredient optional ingredient name filter
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of matching recipe previews
     */
    @GET("api/recipes/search")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> searchRecipes(
            @Query("q") String query,
            @Query("author") String author,
            @Query("ingredient") String ingredient,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Fetches public recipes associated with a specific tag.
     *
     * @param tagName the name of the tag to filter by
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of recipe previews
     */
    @GET("api/recipes/tag/{tagName}")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getRecipesByTag(
            @Path("tagName") String tagName,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Fetches the complete details for a specific recipe.
     *
     * @param id the unique identifier of the recipe
     * @return call yielding the full recipe detail
     */
    @GET("api/recipes/{id}")
    Call<ApiResponse<RecipeResponse>> getRecipeDetail(
            @Path("id") String id
    );

    /**
     * Creates a new recipe authored by the currently authenticated user.
     *
     * @param request the complete recipe payload (metadata, ingredients, instructions, tags)
     * @return call yielding the newly created recipe
     */
    @POST("api/recipes")
    Call<ApiResponse<RecipeResponse>> createRecipe(
            @Body RecipeCreateRequestDTO request
    );

    /**
     * Updates an existing recipe owned by the currently authenticated user.
     *
     * @param id the unique identifier of the recipe to update
     * @param request the complete updated recipe payload
     * @return call yielding the updated recipe response
     */
    @PUT("api/recipes/{id}")
    Call<ApiResponse<RecipeResponse>> updateRecipe(
            @Path("id") String id,
            @Body RecipeCreateRequestDTO request
    );

    /**
     * Fetches every recipe (published or private) authored by the currently authenticated
     * user, for the "My Recipes" screen.
     *
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of the user's own recipes
     */
    @GET("api/recipes/mine")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getMyRecipes(
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Deletes one of the authenticated user's own recipes.
     *
     * @param id the ID of the recipe to delete
     * @return call yielding an empty acknowledgement
     */
    @DELETE("api/recipes/{id}")
    Call<ApiResponse<Void>> deleteRecipe(@Path("id") String id);

    /**
     * Changes only a recipe's visibility (Public/Private) without resubmitting the rest of
     * its fields.
     *
     * @param id the ID of the recipe to update
     * @param request the new visibility
     * @return call yielding the updated recipe
     */
    @PATCH("api/recipes/{id}/visibility")
    Call<ApiResponse<RecipeResponse>> updateRecipeVisibility(
            @Path("id") String id,
            @Body RecipeVisibilityUpdateRequestDTO request
    );


    /**
     * Fetches a page of available tags for the horizontal filter bar.
     *
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of tags
     */
    @GET("api/tags")
    Call<ApiResponse<PagedResponse<TagResponse>>> getAllTags(
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Fetches the most-used tags across all recipes, ranked by descending recipe count.
     *
     * @param limit maximum number of popular tags to return
     * @return call yielding the popular tags ordered by descending usage
     */
    @GET("api/tags/popular")
    Call<ApiResponse<List<TagResponse>>> getPopularTags(
            @Query("limit") int limit
    );

    /**
     * Creates a new custom tag, or returns the existing one if a tag with the same name
     * (case-insensitive) already exists.
     *
     * @param request the tag name payload
     * @return call yielding the created (or matched) tag
     */
    @POST("api/tags/custom")
    Call<ApiResponse<TagResponse>> createCustomTag(
            @Body TagRequestDTO request
    );


    /**
     * Fetches a page of measurement units available for recipe ingredients.
     *
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of units
     */
    @GET("api/units")
    Call<ApiResponse<PagedResponse<UnitResponse>>> getUnits(
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Creates a new measurement unit. Admin-only.
     *
     * @param request unit creation request DTO
     * @return call yielding the created unit
     */
    @POST("api/units")
    Call<ApiResponse<UnitResponse>> createUnit(@Body UnitRequestDTO request);

    /**
     * Deletes a measurement unit by ID. Admin-only.
     *
     * @param id target unit unique identifier
     * @return call acknowledging the deletion
     */
    @DELETE("api/units/{id}")
    Call<ApiResponse<Void>> deleteUnit(@Path("id") String id);


    /**
     * Fetches a page of recipes favorited by the currently authenticated user.
     *
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of the user's favorites
     */
    @GET("api/favorites")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getFavorites(
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Adds a recipe to the user's favorites list.
     *
     * @param recipeId the ID of the recipe to favorite
     * @return call yielding an empty response
     */
    @POST("api/favorites/{recipeId}")
    Call<ApiResponse<Void>> addFavorite(@Path("recipeId") String recipeId);

    /**
     * Removes a recipe from the user's favorites list.
     *
     * @param recipeId the ID of the recipe to unfavorite
     * @return call yielding an empty response
     */
    @DELETE("api/favorites/{recipeId}")
    Call<ApiResponse<Void>> removeFavorite(@Path("recipeId") String recipeId);


    /**
     * Fetches the private personal note attached by the user to a specific recipe.
     *
     * @param recipeId the ID of the recipe
     * @return call yielding the personal note, if any
     */
    @GET("api/notes/recipe/{recipeId}")
    Call<ApiResponse<NoteResponse>> getPersonalNote(
            @Path("recipeId") String recipeId
    );

    /**
     * Fetches every private note the user has attached to a recipe, both the general
     * recipe-wide note and any notes attached to individual instruction steps
     * (distinguished by {@link NoteResponse#instructionId()} being
     * non-null). Used by Cooking Mode to show the right note alongside each step.
     *
     * @param recipeId the ID of the recipe
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of every note (general + per-step) for the recipe
     */
    @GET("api/notes/recipe/{recipeId}/all")
    Call<ApiResponse<PagedResponse<NoteResponse>>> getAllPersonalNotes(
            @Path("recipeId") String recipeId,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Creates or updates a personal note on a recipe (when {@code instructionId} is null) or
     * on a specific instruction step (when it's set).
     *
     * @param request the note payload
     * @return call yielding an empty acknowledgement
     */
    @POST("api/notes")
    Call<ApiResponse<Void>> saveNote(@Body NoteRequestDTO request);

    /**
     * Deletes a personal note.
     *
     * @param noteId the ID of the note to delete
     * @return call yielding an empty acknowledgement
     */
    @DELETE("api/notes/{noteId}")
    Call<ApiResponse<Void>> deleteNote(@Path("noteId") String noteId);


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
     * @param page 0-based page index
     * @param size number of items per page
     * @param q optional search text matched against name/email, or {@code null}
     * @param enabled optional filter by account status, or {@code null} for both
     * @param sortBy one of "firstName", "lastName", "email", "createdAt"
     * @param direction "asc" or "desc"
     * @return call yielding a paged collection of user summaries
     */
    @GET("api/admin/users")
    Call<ApiResponse<PagedResponse<UserResponse>>> getAdminUsers(
            @Query("page") int page,
            @Query("size") int size,
            @Query("q") String q,
            @Query("enabled") Boolean enabled,
            @Query("sortBy") String sortBy,
            @Query("direction") String direction
    );

    /**
     * Fetches a paginated page of reviews currently flagged for moderation, for the Admin
     * Console's Reports tab.
     *
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of reported reviews
     */
    @GET("api/admin/reviews/reported")
    Call<ApiResponse<PagedResponse<ReportedReviewResponse>>> getReportedReviews(
            @Query("page") int page,
            @Query("size") int size
    );

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
     * @param page 0-based page index
     * @param size number of items per page
     * @return call yielding a paged collection of duplicate tag groups
     */
    @GET("api/admin/tags/duplicates")
    Call<ApiResponse<PagedResponse<DuplicateTagGroupResponse>>> getDuplicateTagGroups(
            @Query("page") int page,
            @Query("size") int size
    );

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
