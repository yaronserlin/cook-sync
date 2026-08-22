package com.dtos.request.auth;

import com.dtos.validation.CurrentPassword;

/**
 * Data Transfer Object for self-service account-deletion requests.
 * Requires the user's current password for identity verification prior to starting the
 * 30-day deletion grace period.
 *
 * @param currentPassword the user's active password for re-authentication
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
public record DeleteAccountRequestDTO(
        @CurrentPassword
        String currentPassword
) {
}
