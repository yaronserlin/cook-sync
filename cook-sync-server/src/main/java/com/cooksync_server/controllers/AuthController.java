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
 * Presents the authentication and account-settings surface of the CookSync REST API under
 * {@code /api/auth}: registration and OTP verification, login and refresh-token issuance,
 * session logout, and the authenticated-user profile-management operations covering avatar,
 * personal details, password, email, privacy preferences, and account
 * deactivation/deletion. All business logic is delegated to {@link AuthService}, {@link
 * UserProfileService}, and {@link PasswordService}; this class is responsible only for
 * request/response mapping and wrapping results in {@link ApiResponse}.
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
     * Begins registration for a new account using the submitted credentials. No user record is
     * created and no tokens are issued at this stage; a one-time verification code is emailed to
     * the supplied address, and registration is only finalized through
     * {@link #verifyRegistrationOtp(VerifyRegistrationOtpRequestDTO)}.
     *
     * @param request registration details payload
     * @return response entity carrying the pending-registration payload
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PendingRegistrationResponse>> register(@Valid @RequestBody RegisterRequestDTO request) {
        PendingRegistrationResponse response = authService.register(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Verification code sent to your email"));
    }

    /**
     * Finalizes registration by validating the one-time code emailed for a pending registration.
     * On success, persists the new user account and issues the initial access and refresh
     * tokens.
     *
     * @param request OTP verification payload
     * @return response entity carrying the newly issued authentication payload
     */
    @PostMapping("/verify-registration-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistrationOtp(@Valid @RequestBody VerifyRegistrationOtpRequestDTO request) {
        AuthResponse response = authService.verifyRegistrationOtp(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "User registered successfully"));
    }

    /**
     * Reissues and re-emails a fresh verification code for a pending registration, for use when
     * the previously issued code has expired or was never received.
     *
     * @param request resend-code request payload
     * @return response entity carrying the refreshed pending-registration payload
     */
    @PostMapping("/resend-registration-otp")
    public ResponseEntity<ApiResponse<PendingRegistrationResponse>> resendRegistrationOtp(@Valid @RequestBody ResendRegistrationOtpRequestDTO request) {
        PendingRegistrationResponse response = authService.resendRegistrationOtp(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Verification code resent"));
    }

    /**
     * Authenticates an existing account against the submitted email and password credentials.
     *
     * @param request login credentials payload
     * @return response entity carrying the issued authentication payload
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "User logged in successfully"));
    }

    /**
     * Issues a new JWT access token in exchange for a valid refresh token.
     *
     * @param request refresh-token payload
     * @return response entity carrying the renewed authentication payload
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequestDTO request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Token refreshed successfully"));
    }

    /**
     * Confirms that the caller's JWT access token is still valid and returns the corresponding
     * user profile.
     *
     * @param authentication active Spring Security authentication token
     * @return response entity carrying the current authentication payload
     */
    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<AuthResponse>> validateToken(Authentication authentication) {
        String userEmail = authentication.getName();
        AuthResponse response = authService.validateToken(userEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Token is valid"));
    }

    /**
     * Retrieves the authenticated caller's complete profile, including fields not carried by
     * {@link AuthResponse} such as city, bio, and privacy preferences. Backs the client's
     * Account Details screen when pre-filling its edit form.
     *
     * @param authentication active Spring Security authentication token
     * @return response entity carrying the caller's full profile
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserResponse response = userProfileService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Current user profile retrieved"));
    }

    /**
     * Ends the caller's session by revoking its active refresh token.
     *
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging the logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        String userEmail = authentication.getName();
        authService.logout(userEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "User logged out successfully"));
    }

    /**
     * Replaces the authenticated caller's profile picture URL.
     *
     * @param request avatar-update payload
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging the avatar update
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
     * Updates the authenticated caller's first name, last name, city, and bio.
     *
     * @param request profile-details update payload
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging the profile update
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequestDTO request,
            Authentication authentication) {
        userProfileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Profile updated successfully"));
    }

    /**
     * Changes the authenticated caller's password after verifying the supplied current password.
     *
     * @param request change-password payload
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging the password update
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            Authentication authentication) {
        passwordService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Password updated successfully"));
    }

    /**
     * Changes the authenticated caller's account email address and issues fresh tokens
     * reflecting the new address.
     *
     * @param request email-update payload
     * @param authentication active Spring Security authentication token
     * @return response entity carrying the reissued authentication payload
     */
    @PutMapping("/email")
    public ResponseEntity<ApiResponse<AuthResponse>> updateEmail(
            @Valid @RequestBody EmailUpdateRequestDTO request,
            Authentication authentication) {
        AuthResponse response = userProfileService.updateEmail(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Email updated successfully"));
    }

    /**
     * Deactivates the authenticated caller's account.
     *
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging the deactivation
     */
    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(Authentication authentication) {
        userProfileService.deactivateAccount(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Account deactivated"));
    }

    /**
     * Updates the authenticated caller's public-profile privacy preferences.
     *
     * @param request privacy-settings update payload
     * @param authentication active Spring Security authentication token
     * @return response entity acknowledging the privacy-settings update
     */
    @PutMapping("/privacy")
    public ResponseEntity<ApiResponse<Void>> updatePrivacySettings(
            @Valid @RequestBody PrivacySettingsUpdateRequestDTO request,
            Authentication authentication) {
        userProfileService.updatePrivacySettings(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Privacy settings updated successfully"));
    }

    /**
     * Starts the authenticated caller's 30-day self-service account-deletion grace period after
     * verifying the supplied current password. The account is restored automatically if the user
     * logs back in before the grace period elapses; otherwise it is permanently purged.
     *
     * @param request delete-account payload carrying the current password for verification
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
     * Begins the forgot-password flow by emailing a one-time reset code when the submitted
     * address belongs to a registered account. Always responds with success regardless of
     * whether the address is registered, so the response never discloses account existence.
     *
     * @param request forgot-password request payload
     * @return response entity acknowledging the request
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordService.forgotPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null,
                "If that email is registered, a password reset link has been sent"));
    }

    /**
     * Completes the forgot-password flow by consuming a valid reset code and setting a new
     * account password.
     *
     * @param request reset-password request payload
     * @return response entity acknowledging the password reset
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Password reset successfully"));
    }
}
