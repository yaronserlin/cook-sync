package com.dtos.request.auth;

import com.dtos.validation.CurrentPassword;
import com.dtos.validation.ValidEmail;

/**
 * Request payload for a self-service email-address change, submitted from the Android client's
 * Settings screen and handled by the server's {@code PUT /api/auth/email} endpoint via the user
 * profile service. The current password is re-checked before the new address is accepted.
 *
 * @param newEmail the requested replacement email address; required, must be a well-formed email, and limited to 255 characters
 * @param currentPassword the user's active password, checked to confirm their identity before the change is accepted
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public record EmailUpdateRequestDTO(
        @ValidEmail
        String newEmail,

        @CurrentPassword
        String currentPassword
) {
}
