package com.dtos.response.note;

/**
 * Data Transfer Object representing a private user note on a recipe or instruction step.
 * Encapsulates note unique identifier, recipe mapping, optional step mapping, and note content.
 *
 * @param id unique identifier of the personal note record
 * @param recipeId unique identifier of the parent recipe
 * @param instructionId unique identifier of the specific instruction step, or null for recipe-wide note
 * @param note textual content of the user note
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record NoteResponse(
        String id,
        String recipeId,
        String instructionId,
        String note
) {
}
