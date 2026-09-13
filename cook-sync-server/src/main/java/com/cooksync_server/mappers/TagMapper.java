package com.cooksync_server.mappers;

import com.cooksync_server.entities.ContentTranslation;
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
     * @param tag target Tag entity instance
     * @return populated TagResponse instance or null
     */
    public static TagResponse toResponse(Tag tag) {
        if (tag == null) {
            return null;
        }
        String created = MapperUtils.toIsoStringOrNull(tag.getCreatedAt());
        String updated = MapperUtils.toIsoStringOrNull(tag.getUpdatedAt());
        String name = TranslationAccess.resolve(ContentTranslation.EntityType.TAG_NAME, tag.getId(), tag.getName(), "en").value();
        return new TagResponse(tag.getId(), name, created, updated);
    }

    /**
     * Converts a Tag entity into a TagResponse DTO, reading its translated name from an
     * already-resolved {@link RecipeTranslationBundle} instead of resolving it independently —
     * used when this tag is part of a recipe-wide coordinated translation attempt (see
     * {@link RecipeTranslationCoordinator}).
     *
     * @param tag target Tag entity instance
     * @param bundle the recipe's resolved translation bundle
     * @return populated TagResponse instance or null
     */
    static TagResponse toResponse(Tag tag, RecipeTranslationBundle bundle) {
        if (tag == null) {
            return null;
        }
        String created = MapperUtils.toIsoStringOrNull(tag.getCreatedAt());
        String updated = MapperUtils.toIsoStringOrNull(tag.getUpdatedAt());
        String name = bundle.valueOf(ContentTranslation.EntityType.TAG_NAME, tag.getId(), tag.getName());
        return new TagResponse(tag.getId(), name, created, updated);
    }
}
