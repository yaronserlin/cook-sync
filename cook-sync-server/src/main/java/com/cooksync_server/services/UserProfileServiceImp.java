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
import com.dtos.response.user.UserResponse;

import lombok.RequiredArgsConstructor;

/**
 * Service class handling authenticated-user profile management: reading profile details, and
 * updating avatar, name/city/bio, privacy preferences, account email, and account
 * activation/deletion lifecycle. Registration and login/token concerns live in
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
     * @param userEmail authenticated user email
     * @return the user's full profile
     */
    @Transactional(readOnly = true)
    @Override
    public UserResponse getCurrentUserProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        return UserMapper.toResponse(user);
    }

    /**
     * Fetches a specific user's public profile by user ID.
     *
     * @param userId target user unique identifier
     * @return UserResponse DTO
     */
    @Transactional(readOnly = true)
    @Override
    public UserResponse getUserProfileById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserMapper.toResponse(user);
    }

    /**
     * Updates user avatar picture URL.
     *
     * @param userEmail target user email
     * @param avatarUrl new profile picture URL
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
     * Updates user first name, last name, city, and bio profile details.
     *
     * @param userEmail target user email
     * @param request profile update request DTO
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
     * @param userEmail target user email
     * @param request privacy settings update request DTO
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
     * Updates user account email address following password verification and issues updated tokens.
     *
     * @param userEmail current authenticated user email
     * @param request email update request DTO
     * @return AuthResponse containing updated tokens reflecting new email address
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
     * Deactivates user account (soft delete) and revokes active refresh tokens.
     *
     * @param userEmail target user email
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
     * Starts the 30-day self-service account-deletion grace period following password
     * verification. Distinct from {@link #deactivateAccount(String)}: this also hides the
     * user's reviews and starts the countdown to permanent purge; a plain deactivation does
     * neither. Logging back in within the grace period restores the account via
     * {@link AuthServiceImp#login}.
     *
     * @param userEmail target user email
     * @param request delete-account request DTO carrying the current password for verification
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
