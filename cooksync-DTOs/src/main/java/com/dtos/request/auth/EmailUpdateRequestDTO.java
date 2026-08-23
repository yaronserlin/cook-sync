package com.dtos.request.auth;

import com.dtos.validation.CurrentPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for a self-service email-address change, submitted from the Android client's
 * Settings screen and handled by the server's {@code PUT /api/auth/email} endpoint via the user
 * profile service. The current password is re-checked before the new address is accepted.
 *
 * @param newEmail the requested replacement email address; required, must be a well-formed email, and limited to 255 characters
 * @param currentPassword the user's active password, checked to confirm their identity before the change is accepted
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record EmailUpdateRequestDTO(
        @NotBlank(message = "New email cannot be blank")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String newEmail,

        @CurrentPassword
        String currentPassword
) {
}
