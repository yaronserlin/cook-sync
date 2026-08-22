package com.dtos.response.recipe;

import java.util.List;
import com.dtos.response.tags.TagResponse;

/**
 * Data Transfer Object for lightweight recipe preview cards in feeds and search lists.
 * Excludes heavy relational structures (full ingredient list and instructions) to optimize network bandwidth.
 *
 * @param id unique identifier of the recipe
 * @param authorName full display name of the recipe author
 * @param title display title of the recipe
 * @param description brief summary description
 * @param difficulty skill difficulty level classification
 * @param visibility recipe visibility status
 * @param prepTimeMinutes preparation time in minutes
 * @param cookTimeMinutes active cooking time in minutes
 * @param reviewCount aggregate count of user reviews
 * @param averageRating computed average user rating score
 * @param createdAt ISO formatted creation timestamp string
 * @param tags list of associated category and ingredient tag DTOs
 * @param primaryImageUrl web URL for the cover thumbnail photo
 * @param hasPersonalNote boolean flag indicating whether current user attached a private note
 * @param personalNoteText textual content of the user's private note, if present
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record RecipePreviewResponse(
        String id,
        String authorName,
        String title,
        String description,
        String difficulty,
        String visibility,
        int prepTimeMinutes,
        int cookTimeMinutes,
        int reviewCount,
        Double averageRating,
        String createdAt,
        List<TagResponse> tags,
        String primaryImageUrl,
        boolean hasPersonalNote,
        String personalNoteText
) {
}
