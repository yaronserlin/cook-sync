package com.dtos.request.auth;

import com.dtos.validation.CurrentPassword;

/**
 * Request payload for a self-service account-deletion request, submitted from the Android client's
 * Settings/account-details screen and handled by the server's {@code DELETE /api/auth/account}
 * endpoint through the user profile service, which starts the 30-day deletion grace period after
 * re-verifying the caller's identity.
 *
 * @param currentPassword the user's active password, checked to confirm their identity before the deletion process begins
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
public record DeleteAccountRequestDTO(
        @CurrentPassword
        String currentPassword
) {
}
