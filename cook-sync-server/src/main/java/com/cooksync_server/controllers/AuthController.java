package com.cooksync_server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.services.AuthService;
import com.cooksync_server.services.PasswordService;
import com.cooksync_server.services.UserProfileService;
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
import com.dtos.request.auth.VerifyRegistrationOtpRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.auth.PendingRegistrationResponse;
import com.dtos.response.user.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller exposing user authentication, registration, session management, and profile settings endpoints.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserProfileService userProfileService;
    private final PasswordService passwordService;

    /**
     * Initiates registration for a new account with the provided credentials. No account is
     * created and no tokens are issued yet — a one-time verification code is emailed to the
     * given address, and the registration is only completed by calling
     * {@link #verifyRegistrationOtp(VerifyRegistrationOtpRequestDTO)} with that code.
     *
     * @param request registration details payload DTO
     * @return response entity containing PendingRegistrationResponse payload
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PendingRegistrationResponse>> register(@Valid @RequestBody RegisterRequestDTO request) {
        PendingRegistrationResponse response = authService.register(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Verification code sent to your email"));
    }

    /**
     * Completes registration by validating the OTP code emailed for a pending registration. On
     * success, creates the user account and issues initial access and refresh tokens.
     *
     * @param request OTP verification payload DTO
     * @return response entity containing AuthResponse payload
     */
    @PostMapping("/verify-registration-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistrationOtp(@Valid @RequestBody VerifyRegistrationOtpRequestDTO request) {
        AuthResponse response = authService.verifyRegistrationOtp(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "User registered successfully"));
    }

    /**
     * Regenerates and re-emails a fresh OTP code for an existing pending registration, used when
     * the previous code expired or was not received.
     *
     * @param request resend request payload DTO
     * @return response entity containing PendingRegistrationResponse payload
     */
    @PostMapping("/resend-registration-otp")
    public ResponseEntity<ApiResponse<PendingRegistrationResponse>> resendRegistrationOtp(@Valid @RequestBody ResendRegistrationOtpRequestDTO request) {
        PendingRegistrationResponse response = authService.resendRegistrationOtp(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Verification code resent"));
    }

    /**
     * Authenticates existing user with email and password credentials.
     *
     * @param request login credentials payload DTO
     * @return response entity containing AuthResponse payload
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "User logged in successfully"));
    }

    /**
     * Generates a new JWT access token using a valid refresh token payload.
     *
     * @param request refresh token request payload DTO
     * @return response entity containing renewed AuthResponse payload
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequestDTO request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Token refreshed successfully"));
    }

    /**
     * Validates active JWT authentication token and returns user profile payload.
     *
     * @param authentication active Spring Security authentication token
     * @return response entity containing AuthResponse payload
     */
    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<AuthResponse>> validateToken(Authentication authentication) {
        String userEmail = authentication.getName();
        AuthResponse response = authService.validateToken(userEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Token is valid"));
    }

    /**
     * Fetches the authenticated user's full profile, including fields not carried by
     * {@link AuthResponse} (city, bio, privacy preferences). Used by the client's Account
     * Details screen to pre-fill the edit form.
     *
     * @param authentication active Spring Security authentication token
     * @return response entity containing the current user's full profile
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserResponse response = userProfileService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Current user profile retrieved"));
    }

    /**
     * Invalidates active user refresh token session upon logout.
     *
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        String userEmail = authentication.getName();
        authService.logout(userEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "User logged out successfully"));
    }

    /**
     * Updates profile picture avatar URL for authenticated user.
     *
     * @param request avatar URL update payload DTO
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging avatar update
     */
    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<Void>> updateAvatar(
            @Valid @RequestBody AvatarUpdateRequestDTO request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        userProfileService.updateAvatar(userEmail, request.avatarUrl());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Avatar updated successfully"));
    }

    /**
     * Updates first and last name profile details for authenticated user.
     *
     * @param request profile details update payload DTO
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging profile update
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequestDTO request,
            Authentication authentication) {
        userProfileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Profile updated successfully"));
    }

    /**
     * Updates password for authenticated user following password verification.
     *
     * @param request change password payload DTO
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging password update
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            Authentication authentication) {
        passwordService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Password updated successfully"));
    }

    /**
     * Updates email address for authenticated user and issues new JWT tokens.
     *
     * @param request email update payload DTO
     * @param authentication active Spring Security authentication token
     * @return response entity containing new AuthResponse payload
     */
    @PutMapping("/email")
    public ResponseEntity<ApiResponse<AuthResponse>> updateEmail(
            @Valid @RequestBody EmailUpdateRequestDTO request,
            Authentication authentication) {
        AuthResponse response = userProfileService.updateEmail(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Email updated successfully"));
    }

    /**
     * Deactivates account for authenticated user.
     *
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging account deactivation
     */
    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(Authentication authentication) {
        userProfileService.deactivateAccount(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Account deactivated"));
    }

    /**
     * Updates public-profile privacy preferences for authenticated user.
     *
     * @param request privacy settings update payload DTO
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging the privacy settings update
     */
    @PutMapping("/privacy")
    public ResponseEntity<ApiResponse<Void>> updatePrivacySettings(
            @Valid @RequestBody PrivacySettingsUpdateRequestDTO request,
            Authentication authentication) {
        userProfileService.updatePrivacySettings(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Privacy settings updated successfully"));
    }

    /**
     * Starts the 30-day self-service account-deletion grace period for authenticated user,
     * following password verification. The account is restored automatically if the user logs
     * back in before the grace period lapses; otherwise it is permanently purged.
     *
     * @param request delete-account payload DTO carrying the current password for verification
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging the deletion request
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> requestAccountDeletion(
            @Valid @RequestBody DeleteAccountRequestDTO request,
            Authentication authentication) {
        userProfileService.requestAccountDeletion(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null,
                "Account scheduled for deletion. Log back in within 30 days to cancel."));
    }

    /**
     * Initiates the forgot-password flow by emailing a one-time reset token, if the given
     * email belongs to a registered account. Always returns success regardless of whether the
     * email is registered, so the response never reveals account existence.
     *
     * @param request forgot-password request payload DTO
     * @return response entity acknowledging the request
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordService.forgotPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null,
                "If that email is registered, a password reset link has been sent"));
    }

    /**
     * Completes the forgot-password flow by consuming a valid reset token and setting a new
     * account password.
     *
     * @param request reset-password request payload DTO
     * @return response entity acknowledging the password reset
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Password reset successfully"));
    }
}
