package com.cooksync_server.services;

import com.dtos.request.auth.ChangePasswordRequestDTO;
import com.dtos.request.auth.ForgotPasswordRequestDTO;
import com.dtos.request.auth.ResetPasswordRequestDTO;

/**
 * Defines account password lifecycle operations: authenticated password changes, and the
 * unauthenticated forgot/reset-password one-time-code flow.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public interface PasswordService {

    /**
     * Changes the user's account password after verifying the current password, revoking any
     * existing sessions.
     *
     * @param userEmail target user's email address
     * @param request password change request DTO
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     * @throws com.cooksync_server.exceptions.auth.InvalidCredentialsException if the supplied current password does not match
     */
    void changePassword(String userEmail, ChangePasswordRequestDTO request);

    /**
     * Initiates the forgot-password flow, emailing a one-time reset code when the given email
     * belongs to a registered account. Completes silently for unregistered emails as well, so
     * the caller can never infer whether an address is registered.
     *
     * @param request forgot-password request payload
     */
    void forgotPassword(ForgotPasswordRequestDTO request);

    /**
     * Completes the forgot-password flow by consuming a valid reset code and setting a new
     * account password.
     *
     * @param request reset-password request payload
     * @throws com.cooksync_server.exceptions.auth.InvalidOtpException if the email is unrecognized, no reset code is pending, or the submitted code does not match
     * @throws com.cooksync_server.exceptions.auth.OtpExpiredException if the pending reset code has expired
     * @throws com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException if the incorrect-attempt limit for the pending code has been exceeded
     */
    void resetPassword(ResetPasswordRequestDTO request);
}
