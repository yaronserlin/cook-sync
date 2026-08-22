package com.cooksync_server.mappers;

import com.cooksync_server.entities.User;
import com.dtos.response.user.UserResponse;

/**
 * Mapper utility class transforming User entities into UserResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Converts a User entity into a UserResponse DTO.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param user target User entity instance
     * @return populated UserResponse instance or null
     */
    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        String created = MapperUtils.toIsoStringOrNull(user.getCreatedAt());
        String updated = MapperUtils.toIsoStringOrNull(user.getUpdatedAt());
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isAdmin(),
                user.getAvatarUrl(),
                created,
                updated,
                user.isEnabled(),
                user.getStatus().name(),
                user.getCity(),
                user.getBio(),
                user.isShowRecipesPublicly(),
                user.isShowFavoritesPublicly()
        );
    }
}
