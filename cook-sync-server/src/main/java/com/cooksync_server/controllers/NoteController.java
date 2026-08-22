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

import com.dtos.request.note.NoteRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.note.NoteResponse;
import com.cooksync_server.services.IPersonalNoteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller providing CRUD operations for personal private notes on recipes and instruction steps.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final IPersonalNoteService noteService;

    /**
     * Saves or updates a personal note for a recipe or instruction step.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
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
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Note saved successfully"));
    }

    /**
     * Deletes a personal note by unique note ID.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
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
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Note deleted successfully"));
    }

    /**
     * Retrieves the general recipe-wide personal note for specified recipe ID.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
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
        return ResponseEntity.ok(new ApiResponse<>(true, note, null, "OK"));
    }

    /**
     * Retrieves all personal notes attached to a recipe, including general and step-specific notes.
     *
     * Complexity:
     * Time: O(N) where N is user note count for recipe
     * Space: O(N)
     *
     * @param recipeId target recipe ID
     * @param page page number
     * @param size page size
     * @param authentication active user authentication token
     * @return response entity containing PagedResponse of NoteResponse DTOs
     */
    @GetMapping("/recipe/{recipeId}/all")
    public ResponseEntity<ApiResponse<com.dtos.response.PagedResponse<NoteResponse>>> getNotesForRecipe(
            @PathVariable String recipeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        com.dtos.response.PagedResponse<NoteResponse> notes = noteService.getNotesForRecipe(recipeId, authentication.getName(), page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, notes, null, "OK"));
    }

    /**
     * Retrieves all personal notes created by the authenticated user.
     *
     * Complexity:
     * Time: O(N) where N is user note count
     * Space: O(N)
     *
     * @param page page number
     * @param size page size
     * @param authentication active user authentication token
     * @return response entity containing PagedResponse of NoteResponse DTOs
     */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<com.dtos.response.PagedResponse<NoteResponse>>> getMyNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        com.dtos.response.PagedResponse<NoteResponse> notes = noteService.getMyNotes(authentication.getName(), page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, notes, null, "OK"));
    }
}
