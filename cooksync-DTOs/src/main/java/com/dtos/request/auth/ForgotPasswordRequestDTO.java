package com.dtos.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for initiating the forgot-password flow.
 *
 * @param email the account email to send a password-reset link to
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/08/2026
 */
public record ForgotPasswordRequestDTO(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email
) {
}
