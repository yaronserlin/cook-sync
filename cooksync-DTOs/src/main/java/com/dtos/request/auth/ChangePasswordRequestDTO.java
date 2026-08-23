package com.dtos.request.auth;

import com.dtos.validation.CurrentPassword;
import com.dtos.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for an authenticated user's password-change action, submitted from the Android
 * client's Settings screen and processed by the server's {@code PUT /api/auth/password} endpoint
 * through the password service. The current password is re-verified before the new one is applied.
 *
 * @param currentPassword the user's existing account password, checked to confirm their identity before the change is accepted
 * @param newPassword the desired new password; must be between 6 and 100 characters and contain at least one uppercase letter, one lowercase letter, one digit, and one special character
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record ChangePasswordRequestDTO(
        @CurrentPassword
        String currentPassword,

        @NotBlank(message = "New password cannot be blank")
        @Size(min = 6, message = "New password must be at least 6 characters long")
        @Size(max = 100, message = "New password cannot exceed 100 characters")
        @StrongPassword(message = "New password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
        String newPassword
) {
}
