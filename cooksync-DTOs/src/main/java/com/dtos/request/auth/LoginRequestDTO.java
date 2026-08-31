package com.dtos.request.auth;

import com.dtos.validation.ValidEmail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object representing user authentication credentials for login.
 * Encapsulates email and password validation constraints for authenticating existing users.
 *
 * @param email the user's registered email address, must be valid, non-blank, and at most 255 characters
 * @param password the user's secret password string, must be between 6 and 100 characters
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public record LoginRequestDTO(
        @ValidEmail
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        @Size(max = 100, message = "Password cannot exceed 100 characters")
        String password
) {
}
