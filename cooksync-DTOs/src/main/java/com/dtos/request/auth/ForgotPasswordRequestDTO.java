package com.dtos.request.auth;

import com.dtos.validation.ValidEmail;

/**
 * Data Transfer Object for initiating the forgot-password flow.
 *
 * @param email the account email to send a password-reset link to
 * @author Yaron Serlin
 * @version 1.1
 * @since 05/08/2026
 */
public record ForgotPasswordRequestDTO(
        @ValidEmail
        String email
) {
}
