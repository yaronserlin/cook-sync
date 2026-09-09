package com.cooksync.app.data.datasource.remote;

import com.dtos.request.auth.AvatarUpdateRequestDTO;
import com.dtos.request.common.PageRequestDTO;
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
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.auth.PendingRegistrationResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.user.PublicUserProfileResponse;
import com.dtos.response.user.UserResponse;

import com.cooksync.app.util.constants.ApiEndpoints;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Retrofit contract for authentication and account-management endpoints: registration, login,
 * token refresh, profile/avatar/password/email/privacy updates, and account deletion. Endpoint
 * paths and payload shapes mirror {@code AuthController} on cook-sync-server exactly, since both
 * sides share the same DTOs from the {@code cooksync-DTOs} artifact.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface AuthApiService {

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
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of the user's public recipes
     */
    @GET("api/users/{id}/recipes")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getPublicUserRecipes(
            @Path("id") String id,
            @QueryMap Map<String, String> params
    );

    /**
     * Fetches a page of a user's publicly visible favorites, for their public profile page.
     * Empty if the target user has disabled {@code showFavoritesPublicly}.
     *
     * @param id target user ID
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of the user's public favorites
     */
    @GET("api/users/{id}/favorites")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getPublicUserFavorites(
            @Path("id") String id,
            @QueryMap Map<String, String> params
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
}
