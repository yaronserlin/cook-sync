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
 * Implements account password lifecycle operations: authenticated password changes, and the
 * unauthenticated forgot/reset-password one-time-code flow. Registration and login/token
 * concerns live in {@link AuthServiceImp}; other profile settings live in
 * {@link UserProfileServiceImp}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImp implements PasswordService {

    /** Number of minutes a forgot-password reset code remains valid after being issued. */
    private static final int RESET_TOKEN_VALIDITY_MINUTES = 10;

    /** {@link #RESET_TOKEN_VALIDITY_MINUTES} expressed in milliseconds, for {@link Instant} arithmetic. */
    private static final long RESET_TOKEN_VALIDITY_MS = RESET_TOKEN_VALIDITY_MINUTES * 60 * 1000L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final CredentialVerifier credentialVerifier;

    /**
     * Changes the user's account password after verifying the current password, revoking any
     * existing sessions.
     *
     * @param userEmail target user's email address
     * @param request password change request DTO
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     * @throws InvalidCredentialsException if the supplied current password does not match
     */
    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequestDTO request) {
        User user = credentialVerifier.verifyCurrentPassword(userEmail, request.currentPassword());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.deleteByUserId(user.getId());
    }

    /**
     * Initiates the forgot-password flow: when the submitted email belongs to a registered
     * account, discards any previously issued reset code, generates a fresh one-time 6-digit
     * code, and emails it to the user. Completes silently for unregistered emails as well, so
     * the response never reveals whether an address is registered. Invoking this again for the
     * same account — the client's "resend code" action — simply invalidates the prior code and
     * issues a new one; there is no separate resend endpoint.
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
     * Completes the forgot-password flow: validates the submitted code against the account's
     * active {@link PasswordResetToken} row, updates the account password, deletes the consumed
     * row, and revokes all active sessions for the account. An incorrect code increments the
     * row's attempt count; once {@link OtpCodeGenerator#MAX_ATTEMPTS} incorrect attempts
     * accumulate, the row is invalidated and the user must request a new code. An unregistered
     * email and a known-but-invalid/expired/exhausted code are deliberately indistinguishable to
     * the caller (same exception, same message), preserving the account-existence guarantee made
     * by {@link #forgotPassword}.
     *
     * @param request reset-password request payload
     * @throws InvalidOtpException if the email is unrecognized, no reset code is pending, or the submitted code does not match
     * @throws OtpExpiredException if the pending reset code has expired
     * @throws TooManyOtpAttemptsException if the incorrect-attempt limit for the pending code has just been exceeded by this call
     */
    @Transactional(noRollbackFor = {InvalidOtpException.class, TooManyOtpAttemptsException.class})
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
