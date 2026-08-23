package com.dtos.request.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request body shared between the Android client and the server's {@code POST /api/notes}
 * endpoint for creating or updating a personal note on a recipe or one of its instruction steps.
 *
 * @param recipeId the target recipe unique identifier, must not be null
 * @param instructionId optional instruction step identifier, null if note applies to the overall recipe
 * @param note the user's personal note content, must not be blank
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record NoteRequestDTO(
        @NotNull(message = "Recipe ID is required")
        UUID recipeId,

        UUID instructionId,

        @NotBlank(message = "Note content cannot be empty")
        String note
) {
}