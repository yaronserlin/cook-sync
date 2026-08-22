package com.cooksync_server.mappers;

import com.cooksync_server.entities.Tag;
import com.dtos.response.tags.TagResponse;

/**
 * Mapper utility class transforming Tag entities into TagResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public final class TagMapper {

    private TagMapper() {
    }

    /**
     * Converts a Tag entity into a TagResponse DTO.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param tag target Tag entity instance
     * @return populated TagResponse instance or null
     */
    public static TagResponse toResponse(Tag tag) {
        if (tag == null) {
            return null;
        }
        String created = MapperUtils.toIsoStringOrNull(tag.getCreatedAt());
        String updated = MapperUtils.toIsoStringOrNull(tag.getUpdatedAt());
        return new TagResponse(tag.getId(), tag.getName(), created, updated);
    }
}
