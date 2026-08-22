package com.dtos.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object representing user authentication credentials for login.
 * Encapsulates email and password validation constraints for authenticating existing users.
 *
 * @param email the user's registered email address, must be valid and non-blank
 * @param password the user's secret password string, must be between 6 and 100 characters
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record LoginRequestDTO(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        @Size(max = 100, message = "Password cannot exceed 100 characters")
        String password
) {
}
