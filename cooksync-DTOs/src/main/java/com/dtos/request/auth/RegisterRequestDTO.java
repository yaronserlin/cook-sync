package com.dtos.request.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration requests.
 * Encapsulates user profile details and password policy constraints required to register a new account.
 *
 * @param firstName the user's first name, must be between 2 and 50 characters
 * @param lastName the user's last name, must be between 2 and 50 characters
 * @param email the user's email address, must be valid and unique across the system
 * @param password the user's raw password, requiring uppercase, lowercase, numeric, and special characters
 * @param termsAccepted whether the user accepted the terms of use; registration is rejected unless {@code true}
 * @param marketingOptIn whether the user opted in to marketing communications; optional, defaults to {@code false}
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record RegisterRequestDTO(
        @NotBlank(message = "First name cannot be blank")
        @Size(min = 2, message = "First name must be at least 2 characters long")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        @Size(min = 2, message = "Last name must be at least 2 characters long")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        String lastName,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        @Size(max = 100, message = "Password cannot exceed 100 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String password,

        @AssertTrue(message = "You must accept the terms of use to create an account")
        boolean termsAccepted,

        boolean marketingOptIn
) {
}
