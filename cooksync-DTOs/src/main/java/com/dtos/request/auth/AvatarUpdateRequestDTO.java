package com.dtos.request.auth;

import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for updating a user's avatar image URL.
 * Encapsulates validation constraints for profile picture updates.
 *
 * @param avatarUrl the web URL pointing to the user's uploaded avatar image, or null to clear
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record AvatarUpdateRequestDTO(
        @Size(max = 2000, message = "Avatar URL cannot exceed 2000 characters")
        String avatarUrl
) {
}
