package com.cooksync_server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.constants.PaginationDefaults;
import com.dtos.request.note.NoteRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.note.NoteResponse;
import com.cooksync_server.services.PersonalNoteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST entry point for the personal-notes feature: private, per-user text notes attached either
 * to a whole recipe or to one of its instruction steps. Delegates all persistence and business
 * rules to {@link PersonalNoteService}; this class is limited to request/response mapping and
 * resolving the authenticated caller's identity from {@link Authentication}. Consumed by the
 * Android client's {@code RecipeRepository} (via {@code ApiService}) from the recipe detail and
 * cooking-mode screens.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final PersonalNoteService noteService;

    /**
     * Saves or updates a personal note for a recipe or instruction step.
     *
     * @param request note creation or update request DTO
     * @param authentication active user authentication token
     * @return response entity acknowledging note save operation
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveNote(
            @Valid @RequestBody NoteRequestDTO request,
            Authentication authentication) {
        noteService.saveNote(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Note saved successfully"));
    }

    /**
     * Deletes a personal note by unique note ID.
     *
     * @param noteId target note ID
     * @param authentication active user authentication token
     * @return response entity acknowledging note deletion
     */
    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable String noteId,
            Authentication authentication) {
        noteService.deleteNote(noteId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Note deleted successfully"));
    }

    /**
     * Retrieves the general recipe-wide personal note for specified recipe ID.
     *
     * @param recipeId target recipe ID
     * @param authentication active user authentication token
     * @return response entity containing NoteResponse DTO
     */
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<NoteResponse>> getNote(
            @PathVariable String recipeId,
            Authentication authentication) {
        NoteResponse note = noteService.getNote(recipeId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(note, "OK"));
    }

    /**
     * Retrieves all personal notes attached to a recipe, including general and step-specific notes.
     *
     * @param recipeId target recipe ID
     * @param page page number
     * @param size page size
     * @param authentication active user authentication token
     * @return response entity containing PagedResponse of NoteResponse DTOs
     */
    @GetMapping("/recipe/{recipeId}/all")
    public ResponseEntity<ApiResponse<PagedResponse<NoteResponse>>> getNotesForRecipe(
            @PathVariable String recipeId,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size,
            Authentication authentication) {
        PagedResponse<NoteResponse> notes = noteService.getNotesForRecipe(recipeId, authentication.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(notes, "OK"));
    }
}
