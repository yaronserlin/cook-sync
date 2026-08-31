package com.dtos.request.auth;

import com.dtos.validation.ValidEmail;

/**
 * Data Transfer Object for requesting a fresh OTP code for a pending registration,
 * used when the previously issued code expired or was not received.
 *
 * @param email the email address the pending registration belongs to
 * @author Yaron Serlin
 * @version 1.1
 * @since 13/08/2026
 */
public record ResendRegistrationOtpRequestDTO(
        @ValidEmail
        String email
) {
}
