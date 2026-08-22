package com.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for authenticated user password change requests.
 * Requires verification of the current password alongside the new complex password.
 *
 * @param currentPassword the user's existing account password for identity verification
 * @param newPassword the new secret password meeting system security criteria
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record ChangePasswordRequestDTO(
        @NotBlank(message = "Current password is required")
        @Size(max = 100, message = "Current password cannot exceed 100 characters")
        String currentPassword,

        @NotBlank(message = "New password cannot be blank")
        @Size(min = 6, message = "New password must be at least 6 characters long")
        @Size(max = 100, message = "New password cannot exceed 100 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$",
                message = "New password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String newPassword
) {
}
