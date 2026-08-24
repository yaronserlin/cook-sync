package com.cooksync_server.services;

import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.request.auth.VerifyEmailChangeOtpRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.user.PublicUserProfileResponse;
import com.dtos.response.user.UserResponse;

/**
 * Defines authenticated-user profile management: retrieving profile details, and updating
 * avatar, name/city/bio, privacy preferences, account email, and the
 * account-activation/deletion lifecycle.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public interface UserProfileService {

    /**
     * Fetches the authenticated user's full profile, including fields not carried by
     * {@link AuthResponse} (city, bio, privacy preferences).
     *
     * @param userEmail authenticated user's email address
     * @return the user's full profile
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    UserResponse getCurrentUserProfile(String userEmail);

    /**
     * Fetches a specific user's public profile by user ID, deliberately excluding fields
     * (email, admin status, account status) not appropriate to disclose to another user.
     *
     * @param userId target user's unique identifier
     * @return the matching user's public profile
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userId}
     */
    PublicUserProfileResponse getUserProfileById(String userId);

    /**
     * Updates the user's avatar picture URL.
     *
     * @param userEmail target user's email address
     * @param avatarUrl new profile picture URL
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    void updateAvatar(String userEmail, String avatarUrl);

    /**
     * Updates the user's first name, last name, city, and bio.
     *
     * @param userEmail target user's email address
     * @param request profile update request DTO
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    void updateProfile(String userEmail, ProfileUpdateRequestDTO request);

    /**
     * Updates the user's public-profile privacy preferences.
     *
     * @param userEmail target user's email address
     * @param request privacy settings update request DTO
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    void updatePrivacySettings(String userEmail, PrivacySettingsUpdateRequestDTO request);

    /**
     * Begins a self-service email-address change after verifying the current password: checks
     * the requested address isn't already registered, then emails a one-time 6-digit
     * verification code to that new address. The account email is not changed yet — only
     * {@link #confirmEmailChange(String, VerifyEmailChangeOtpRequestDTO)} applies it. Calling
     * this again (a "resend") simply invalidates any previously issued code and issues a fresh
     * one.
     *
     * @param userEmail current authenticated user's email address
     * @param request email update request DTO
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     * @throws com.cooksync_server.exceptions.auth.InvalidCredentialsException if the supplied current password does not match
     * @throws com.cooksync_server.exceptions.auth.UserAlreadyExistsException if the requested new email is already registered to another account
     */
    void requestEmailChange(String userEmail, EmailUpdateRequestDTO request);

    /**
     * Completes a self-service email-address change by validating the one-time code emailed to
     * the pending new address, applying the change, and issuing refreshed tokens reflecting it.
     *
     * @param userEmail current authenticated user's email address
     * @param request OTP verification request DTO
     * @return authentication payload carrying tokens issued for the new email address
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     * @throws com.cooksync_server.exceptions.auth.InvalidOtpException if no email-change code is pending, or the submitted code does not match
     * @throws com.cooksync_server.exceptions.auth.OtpExpiredException if the pending code has expired
     * @throws com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException if the incorrect-attempt limit for the pending code has just been exceeded by this call
     * @throws com.cooksync_server.exceptions.auth.UserAlreadyExistsException if the pending new email became registered to another account in the meantime
     */
    AuthResponse confirmEmailChange(String userEmail, VerifyEmailChangeOtpRequestDTO request);

    /**
     * Deactivates the user's account (soft delete) and revokes active refresh tokens.
     *
     * @param userEmail target user's email address
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    void deactivateAccount(String userEmail);

    /**
     * Starts the 30-day self-service account-deletion grace period after verifying the current
     * password.
     *
     * @param userEmail target user's email address
     * @param request delete-account request DTO carrying the current password for verification
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     * @throws com.cooksync_server.exceptions.auth.InvalidCredentialsException if the supplied current password does not match
     */
    void requestAccountDeletion(String userEmail, DeleteAccountRequestDTO request);
}
