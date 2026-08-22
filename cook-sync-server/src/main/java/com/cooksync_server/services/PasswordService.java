package com.cooksync_server.services;

import com.dtos.request.auth.ChangePasswordRequestDTO;
import com.dtos.request.auth.ForgotPasswordRequestDTO;
import com.dtos.request.auth.ResetPasswordRequestDTO;

/**
 * Service interface for account password lifecycle: authenticated password changes, and the
 * unauthenticated forgot/reset-password OTP flow.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public interface PasswordService {

    /**
     * Changes the user's account password following current-password verification, revoking
     * existing sessions.
     *
     * @param userEmail target user email
     * @param request password change request DTO
     */
    void changePassword(String userEmail, ChangePasswordRequestDTO request);

    /**
     * Initiates the forgot-password flow, emailing a one-time reset token if the given email
     * belongs to a registered account.
     *
     * @param request forgot-password request payload
     */
    void forgotPassword(ForgotPasswordRequestDTO request);

    /**
     * Completes the forgot-password flow by consuming a valid reset token and setting a new
     * account password.
     *
     * @param request reset-password request payload
     */
    void resetPassword(ResetPasswordRequestDTO request);
}
