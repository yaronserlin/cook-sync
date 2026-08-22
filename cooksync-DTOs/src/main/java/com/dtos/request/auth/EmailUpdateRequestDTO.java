package com.dtos.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for self-service email change requests.
 * Requires the user's current password for identity verification prior to updating the email address.
 *
 * @param newEmail the target new email address, must be valid and up to 255 characters
 * @param currentPassword the user's active password for re-authentication
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record EmailUpdateRequestDTO(
        @NotBlank(message = "New email cannot be blank")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String newEmail,

        @NotBlank(message = "Current password is required")
        @Size(max = 100, message = "Current password cannot exceed 100 characters")
        String currentPassword
) {
}
