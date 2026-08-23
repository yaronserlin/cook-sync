package com.cooksync_server.services;

import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.response.auth.AuthResponse;
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
     * Fetches a specific user's public profile by user ID.
     *
     * @param userId target user's unique identifier
     * @return the matching user's profile
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userId}
     */
    UserResponse getUserProfileById(String userId);

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
     * Updates the user's account email address after verifying the current password, and issues
     * refreshed tokens reflecting the change.
     *
     * @param userEmail current authenticated user's email address
     * @param request email update request DTO
     * @return authentication payload carrying tokens issued for the new email address
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     * @throws com.cooksync_server.exceptions.auth.InvalidCredentialsException if the supplied current password does not match
     * @throws com.cooksync_server.exceptions.auth.UserAlreadyExistsException if the requested new email is already registered to another account
     */
    AuthResponse updateEmail(String userEmail, EmailUpdateRequestDTO request);

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
