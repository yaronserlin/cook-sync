package com.cooksync_server.mappers;

import com.cooksync_server.entities.User;
import com.dtos.response.user.UserResponse;

/**
 * Mapper utility converting {@link User} JPA entities into {@link UserResponse} DTOs for
 * outbound API responses.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Converts a {@link User} entity into its {@link UserResponse} DTO representation.
     *
     * @param user source User entity, may be {@code null}
     * @return the populated UserResponse, or {@code null} if {@code user} is {@code null}
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
