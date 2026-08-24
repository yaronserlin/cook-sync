package com.dtos.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for requesting a fresh OTP code for a pending registration,
 * used when the previously issued code expired or was not received.
 *
 * @param email the email address the pending registration belongs to
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public record ResendRegistrationOtpRequestDTO(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email
) {
}
