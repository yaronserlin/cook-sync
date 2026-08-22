package com.cooksync_server.services;

import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.user.UserResponse;

/**
 * Service interface for authenticated-user profile management: reading profile details, and
 * updating avatar, name/city/bio, privacy preferences, account email, and account
 * activation/deletion lifecycle.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public interface IUserProfileService {

    /**
     * Fetches the authenticated user's full profile, including fields not carried by
     * {@link AuthResponse} (city, bio, privacy preferences).
     *
     * @param userEmail authenticated user email
     * @return the user's full profile
     */
    UserResponse getCurrentUserProfile(String userEmail);

    /**
     * Fetches a specific user's public profile by user ID.
     *
     * @param userId target user unique identifier
     * @return UserResponse DTO
     */
    UserResponse getUserProfileById(String userId);

    /**
     * Updates the user's avatar picture URL.
     *
     * @param userEmail target user email
     * @param avatarUrl new profile picture URL
     */
    void updateAvatar(String userEmail, String avatarUrl);

    /**
     * Updates the user's first name, last name, city, and bio profile details.
     *
     * @param userEmail target user email
     * @param request profile update request DTO
     */
    void updateProfile(String userEmail, ProfileUpdateRequestDTO request);

    /**
     * Updates the user's public-profile privacy preferences.
     *
     * @param userEmail target user email
     * @param request privacy settings update request DTO
     */
    void updatePrivacySettings(String userEmail, PrivacySettingsUpdateRequestDTO request);

    /**
     * Updates the user's account email address following password verification and issues
     * updated tokens.
     *
     * @param userEmail current authenticated user email
     * @param request email update request DTO
     * @return AuthResponse containing updated tokens reflecting the new email address
     */
    AuthResponse updateEmail(String userEmail, EmailUpdateRequestDTO request);

    /**
     * Deactivates the user's account (soft delete) and revokes active refresh tokens.
     *
     * @param userEmail target user email
     */
    void deactivateAccount(String userEmail);

    /**
     * Starts the 30-day self-service account-deletion grace period following password
     * verification.
     *
     * @param userEmail target user email
     * @param request delete-account request DTO carrying the current password for verification
     */
    void requestAccountDeletion(String userEmail, DeleteAccountRequestDTO request);
}
