package com.dtos.response.note;

/**
 * Response body shared between the server's note endpoints and the Android client for a single
 * private note. A null {@code instructionId} identifies a recipe-wide note; a non-null one scopes
 * the note to that instruction step (used by the client to key its per-step note lookup).
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
