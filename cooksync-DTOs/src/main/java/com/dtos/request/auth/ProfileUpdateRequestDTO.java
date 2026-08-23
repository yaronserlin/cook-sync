package com.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a user's personal profile information. Submitted by the Android
 * client's Settings/account-details screen when the user edits their display name, city, or bio,
 * and carried to the server's {@code PUT /api/auth/profile} endpoint, where it is validated and
 * applied via the profile-update flow of the user profile service.
 *
 * @param firstName the user's updated first name; required and limited to 255 characters
 * @param lastName the user's updated last name; required and limited to 255 characters
 * @param city the user's updated city shown on their public profile; optional, limited to 255 characters
 * @param bio the user's updated short biography; optional, limited to 1000 characters
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public record ProfileUpdateRequestDTO(
        @NotBlank(message = "First name is required")
        @Size(max = 255, message = "First name cannot exceed 255 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 255, message = "Last name cannot exceed 255 characters")
        String lastName,

        @Size(max = 255, message = "City cannot exceed 255 characters")
        String city,

        @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
        String bio
) {
}
