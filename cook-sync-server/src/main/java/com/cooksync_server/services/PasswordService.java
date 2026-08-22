package com.cooksync_server.services;

import java.time.Instant;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtos.request.auth.ChangePasswordRequestDTO;
import com.dtos.request.auth.ForgotPasswordRequestDTO;
import com.dtos.request.auth.ResetPasswordRequestDTO;
import com.cooksync_server.entities.PasswordResetToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.exceptions.auth.InvalidOtpException;
import com.cooksync_server.exceptions.auth.OtpExpiredException;
import com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException;
import com.cooksync_server.repositories.PasswordResetTokenRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class handling account password lifecycle: authenticated password changes, and the
 * unauthenticated forgot/reset-password OTP flow. Registration and login/token concerns live in
 * {@link AuthService}; other profile settings live in {@link UserProfileService}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService implements IPasswordService {

    /** How many minutes a forgot-password reset code remains valid after being issued. */
    private static final int RESET_TOKEN_VALIDITY_MINUTES = 10;

    /** {@link #RESET_TOKEN_VALIDITY_MINUTES} expressed in milliseconds, for {@link Instant} arithmetic. */
    private static final long RESET_TOKEN_VALIDITY_MS = RESET_TOKEN_VALIDITY_MINUTES * 60 * 1000L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final IRefreshTokenService refreshTokenService;
    private final IEmailService emailService;

    /**
     * Changes user account password following verification of current password, revoking existing sessions.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param userEmail target user email
     * @param request password change request DTO
     */
    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.deleteByUserId(user.getId());
    }

    /**
     * Initiates the forgot-password flow: if the email belongs to a registered account, issues a
     * fresh one-time 6-digit reset code and emails it to the user. Always succeeds silently for
     * unknown emails as well, so the response never reveals whether an address is registered.
     * Calling this again for the same account (e.g. the client's "resend code" action) simply
     * invalidates any prior code and issues a fresh one — there is no separate resend endpoint.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param request forgot-password request payload
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.email());
        if (optionalUser.isEmpty()) {
            log.info("Forgot-password requested for unknown email: {}", request.email());
            return;
        }

        User user = optionalUser.get();
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String resetCode = OtpCodeGenerator.generate();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(resetCode))
                .expiryDate(Instant.now().plusMillis(RESET_TOKEN_VALIDITY_MS))
                .build();
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), resetCode, RESET_TOKEN_VALIDITY_MINUTES);
        log.info("Password reset code issued for user ID: {}", user.getId());
    }

    /**
     * Completes the forgot-password flow: validates the submitted reset code against the
     * account's active {@link PasswordResetToken} row, updates the account password, deletes the
     * consumed row, and revokes all active sessions for the account. Incorrect codes increment
     * the row's attempt count; once {@link OtpCodeGenerator#MAX_ATTEMPTS} incorrect attempts
     * accumulate, the row is invalidated and the user must request a new code. An unknown email
     * and a known-but-invalid/expired/exhausted code are deliberately indistinguishable to the
     * caller (same exception, same message), preserving {@link #forgotPassword}'s guarantee that
     * account existence is never revealed.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param request reset-password request payload
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired reset code"));

        PasswordResetToken resetToken = passwordResetTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired reset code"));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new OtpExpiredException("Reset code has expired");
        }

        if (!passwordEncoder.matches(request.code(), resetToken.getCodeHash())) {
            resetToken.setAttemptCount(resetToken.getAttemptCount() + 1);
            if (resetToken.getAttemptCount() >= OtpCodeGenerator.MAX_ATTEMPTS) {
                passwordResetTokenRepository.delete(resetToken);
                throw new TooManyOtpAttemptsException("Too many incorrect attempts. Please request a new code.");
            }
            passwordResetTokenRepository.save(resetToken);
            throw new InvalidOtpException("Incorrect reset code");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        refreshTokenService.deleteByUserId(user.getId());
        log.info("Password reset completed for user ID: {}", user.getId());
    }
}
