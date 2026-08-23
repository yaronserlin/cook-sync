package com.dtos.request.auth;

import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a user's avatar image reference. Sent by the Android client after
 * the account-details/settings screen uploads a new profile picture to Cloudinary and receives its
 * hosted URL back; the server's {@code PUT /api/auth/avatar} endpoint persists the URL through the
 * user profile service.
 *
 * @param avatarUrl the hosted URL of the user's uploaded avatar image, limited to 2000 characters; may be null to clear it
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record AvatarUpdateRequestDTO(
        @Size(max = 2000, message = "Avatar URL cannot exceed 2000 characters")
        String avatarUrl
) {
}
