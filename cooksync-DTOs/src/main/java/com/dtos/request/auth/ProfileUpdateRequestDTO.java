package com.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for updating user personal profile information.
 * Encapsulates validated fields for first name, last name, city, and bio updates.
 *
 * @param firstName the user's updated first name, must not be blank and up to 255 characters
 * @param lastName the user's updated last name, must not be blank and up to 255 characters
 * @param city the user's updated city, shown on their public profile; optional, up to 255 characters
 * @param bio the user's updated short biography; optional, up to 1000 characters
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
