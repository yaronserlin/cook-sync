package com.cooksync_server.services;

import com.dtos.request.note.NoteRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.note.NoteResponse;

/**
 * Service contract for the personal-notes feature: business rules and persistence access for a
 * user's private notes on recipes and individual instruction steps. Implemented by
 * {@link PersonalNoteServiceImp} and consumed by {@link com.cooksync_server.controllers.NoteController},
 * keeping the REST layer free of persistence and authorization concerns.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface PersonalNoteService {

    /**
     * Saves or updates a personal private note for a recipe or a specific instruction step.
     *
     * @param request note creation or update request DTO
     * @param userEmail authenticated user email address
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if the user or recipe cannot be found, or if
     *         {@code request.instructionId()} doesn't identify a step belonging to that recipe
     */
    void saveNote(NoteRequestDTO request, String userEmail);

    /**
     * Retrieves the general recipe-level personal note (not tied to a specific instruction step).
     *
     * @param recipeId target recipe ID
     * @param userEmail authenticated user email address
     * @return NoteResponse DTO, or null if no note is attached
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    NoteResponse getNote(String recipeId, String userEmail);

    /**
     * Retrieves all personal notes the user has created for a recipe (general and step-specific).
     *
     * @param recipeId target recipe ID
     * @param userEmail authenticated user email address
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse of NoteResponse DTOs
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    PagedResponse<NoteResponse> getNotesForRecipe(String recipeId, String userEmail, int page, int size);

    /**
     * Deletes a personal note following author verification.
     *
     * @param noteId target note ID
     * @param userEmail authenticated user email address
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no note matches {@code noteId}
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if {@code userEmail} does not match the note's author
     */
    void deleteNote(String noteId, String userEmail);
}
