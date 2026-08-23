package com.cooksync_server.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.config.JwtUtil;
import com.cooksync_server.entities.RefreshToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.exceptions.auth.UserAlreadyExistsException;
import com.cooksync_server.mappers.UserMapper;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.user.PublicUserProfileResponse;
import com.dtos.response.user.UserResponse;

import lombok.RequiredArgsConstructor;

/**
 * Implements authenticated-user profile management: retrieving profile details, and updating
 * avatar, name/city/bio, privacy preferences, account email, and the
 * account-activation/deletion lifecycle. Registration and login/token concerns live in
 * {@link AuthServiceImp}; password change/reset lives in {@link PasswordServiceImp}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImp implements UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final CloudinaryService cloudinaryService;
    private final AccountDeletionService accountDeletionService;

    /**
     * Fetches the authenticated user's full profile, including fields not carried by
     * {@link AuthResponse} (city, bio, privacy preferences).
     *
     * @param userEmail authenticated user's email address
     * @return the user's full profile
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     */
    @Transactional(readOnly = true)
    @Override
    public UserResponse getCurrentUserProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        return UserMapper.toResponse(user);
    }

    /**
     * Fetches a specific user's public profile by user ID, deliberately excluding fields
     * (email, admin status, account status) not appropriate to disclose to another user.
     *
     * @param userId target user's unique identifier
     * @return the matching user's public profile
     * @throws ResourceNotFoundException if no user matches {@code userId}
     */
    @Transactional(readOnly = true)
    @Override
    public PublicUserProfileResponse getUserProfileById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserMapper.toPublicProfileResponse(user);
    }

    /**
     * Replaces the user's avatar picture URL, deleting the previous Cloudinary asset when one
     * existed and differs from the new URL.
     *
     * @param userEmail target user's email address
     * @param avatarUrl new profile picture URL
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     */
    @Transactional
    @Override
    public void updateAvatar(String userEmail, String avatarUrl) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        String oldAvatarUrl = user.getAvatarUrl();
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank() && !oldAvatarUrl.equals(avatarUrl)) {
            cloudinaryService.deleteImage(oldAvatarUrl);
        }

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
    }

    /**
     * Updates the user's first name, last name, city, and bio.
     *
     * @param userEmail target user's email address
     * @param request profile update request DTO
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     */
    @Transactional
    @Override
    public void updateProfile(String userEmail, ProfileUpdateRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setCity(request.city());
        user.setBio(request.bio());
        userRepository.save(user);
    }

    /**
     * Updates the user's public-profile privacy preferences.
     *
     * @param userEmail target user's email address
     * @param request privacy settings update request DTO
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     */
    @Transactional
    @Override
    public void updatePrivacySettings(String userEmail, PrivacySettingsUpdateRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        user.setShowRecipesPublicly(request.showRecipesPublicly());
        user.setShowFavoritesPublicly(request.showFavoritesPublicly());
        userRepository.save(user);
    }

    /**
     * Updates the user's account email address after verifying the current password, and issues
     * refreshed tokens reflecting the change. Rejects the change when the requested address is
     * already registered to another account, whether that is caught by the upfront lookup or by
     * a unique-constraint violation surfaced at save time under concurrent registration.
     *
     * @param userEmail current authenticated user's email address
     * @param request email update request DTO
     * @return authentication payload carrying tokens issued for the new email address
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     * @throws InvalidCredentialsException if the supplied current password does not match
     * @throws UserAlreadyExistsException if the requested new email is already registered to another account
     */
    @Transactional
    @Override
    public AuthResponse updateEmail(String userEmail, EmailUpdateRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        String newEmail = request.newEmail().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        user.setEmail(newEmail);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.isAdmin());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return new AuthResponse(token, refreshToken.getToken(), user.getId(), user.getFirstName(), user.getLastName(), user.isAdmin(), user.getAvatarUrl());
    }

    /**
     * Deactivates the user's account (soft delete) and revokes active refresh tokens.
     *
     * @param userEmail target user's email address
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     */
    @Transactional
    @Override
    public void deactivateAccount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        user.setEnabled(false);
        user.setStatus(User.AccountStatus.DEACTIVATED);
        userRepository.save(user);
        refreshTokenService.deleteByUserId(user.getId());
    }

    /**
     * Starts the 30-day self-service account-deletion grace period after verifying the current
     * password. Distinct from {@link #deactivateAccount(String)}: this additionally hides the
     * user's reviews and starts the countdown to permanent purge, neither of which a plain
     * deactivation does. Logging back in within the grace period restores the account via
     * {@link AuthServiceImp#login}.
     *
     * @param userEmail target user's email address
     * @param request delete-account request DTO carrying the current password for verification
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     * @throws InvalidCredentialsException if the supplied current password does not match
     */
    @Transactional
    @Override
    public void requestAccountDeletion(String userEmail, DeleteAccountRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        accountDeletionService.requestDeletion(user);
    }
}
