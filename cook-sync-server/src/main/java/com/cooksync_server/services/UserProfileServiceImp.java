package com.cooksync_server.services;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.entities.EmailChangeToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.exceptions.auth.InvalidOtpException;
import com.cooksync_server.exceptions.auth.OtpExpiredException;
import com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException;
import com.cooksync_server.exceptions.auth.UserAlreadyExistsException;
import com.cooksync_server.mappers.UserMapper;
import com.cooksync_server.repositories.EmailChangeTokenRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.request.auth.VerifyEmailChangeOtpRequestDTO;
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

    /** Number of minutes an email-change verification code remains valid after being issued. */
    private static final int EMAIL_CHANGE_TOKEN_VALIDITY_MINUTES = 10;

    /** {@link #EMAIL_CHANGE_TOKEN_VALIDITY_MINUTES} expressed in milliseconds, for {@link Instant} arithmetic. */
    private static final long EMAIL_CHANGE_TOKEN_VALIDITY_MS = EMAIL_CHANGE_TOKEN_VALIDITY_MINUTES * 60 * 1000L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final CloudinaryService cloudinaryService;
    private final AccountDeletionService accountDeletionService;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final EmailService emailService;
    private final SessionIssuer sessionIssuer;
    private final CredentialVerifier credentialVerifier;

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
     * Begins a self-service email-address change after verifying the current password: rejects
     * the change up front when the requested address is already registered to another account,
     * then discards any previously issued email-change code and emails a fresh one-time 6-digit
     * code to the new address. The account's email is left untouched until
     * {@link #confirmEmailChange(String, VerifyEmailChangeOtpRequestDTO)} validates that code.
     * Invoking this again for the same account — the client's "resend code" action — simply
     * invalidates the prior code and issues a new one; there is no separate resend endpoint.
     *
     * @param userEmail current authenticated user's email address
     * @param request email update request DTO
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     * @throws InvalidCredentialsException if the supplied current password does not match
     * @throws UserAlreadyExistsException if the requested new email is already registered to another account
     */
    @Transactional
    @Override
    public void requestEmailChange(String userEmail, EmailUpdateRequestDTO request) {
        User user = credentialVerifier.verifyCurrentPassword(userEmail, request.currentPassword());

        String newEmail = request.newEmail().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        emailChangeTokenRepository.deleteByUserId(user.getId());

        String code = OtpCodeGenerator.generate();
        EmailChangeToken changeToken = EmailChangeToken.builder()
                .user(user)
                .newEmail(newEmail)
                .codeHash(passwordEncoder.encode(code))
                .expiryDate(Instant.now().plusMillis(EMAIL_CHANGE_TOKEN_VALIDITY_MS))
                .build();
        emailChangeTokenRepository.save(changeToken);

        emailService.sendOtpEmail(newEmail, code, EMAIL_CHANGE_TOKEN_VALIDITY_MINUTES);
    }

    /**
     * Completes a self-service email-address change: validates the submitted code against the
     * account's active {@link EmailChangeToken} row, applies the pending new email, deletes the
     * consumed row, and issues fresh tokens reflecting the change. An incorrect code increments
     * the row's attempt count; once {@link OtpCodeGenerator#MAX_ATTEMPTS} incorrect attempts
     * accumulate, the row is invalidated and the user must request a new code. The pending new
     * email's availability is re-checked here as well as at request time, since another account
     * could have registered it in the interim.
     *
     * @param userEmail current authenticated user's email address
     * @param request OTP verification request DTO
     * @return authentication payload carrying tokens issued for the new email address
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     * @throws InvalidOtpException if no email-change code is pending, or the submitted code does not match
     * @throws OtpExpiredException if the pending code has expired
     * @throws TooManyOtpAttemptsException if the incorrect-attempt limit for the pending code has just been exceeded by this call
     * @throws UserAlreadyExistsException if the pending new email became registered to another account in the meantime
     */
    @Transactional(noRollbackFor = {InvalidOtpException.class, TooManyOtpAttemptsException.class})
    @Override
    public AuthResponse confirmEmailChange(String userEmail, VerifyEmailChangeOtpRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        EmailChangeToken changeToken = emailChangeTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired verification code"));

        if (changeToken.getExpiryDate().isBefore(Instant.now())) {
            throw new OtpExpiredException("Verification code has expired");
        }

        if (!passwordEncoder.matches(request.code(), changeToken.getCodeHash())) {
            changeToken.setAttemptCount(changeToken.getAttemptCount() + 1);
            if (changeToken.getAttemptCount() >= OtpCodeGenerator.MAX_ATTEMPTS) {
                emailChangeTokenRepository.delete(changeToken);
                throw new TooManyOtpAttemptsException("Too many incorrect attempts. Please request a new code.");
            }
            emailChangeTokenRepository.save(changeToken);
            throw new InvalidOtpException("Incorrect verification code");
        }

        String newEmail = changeToken.getNewEmail();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        user.setEmail(newEmail);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        emailChangeTokenRepository.delete(changeToken);

        return sessionIssuer.issue(user);
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
        User user = credentialVerifier.verifyCurrentPassword(userEmail, request.currentPassword());
        accountDeletionService.requestDeletion(user);
    }
}
