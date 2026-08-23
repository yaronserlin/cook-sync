package com.cooksync_server.services;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtos.request.note.NoteRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.note.NoteResponse;
import com.cooksync_server.entities.FavoriteRecipe;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.entities.PersonalInstructionNote;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.InstructionRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Default implementation of {@link PersonalNoteService}: manages user private notes on recipes
 * and step-by-step instructions, backed by {@link PersonalInstructionNoteRepository}. Cross-checks
 * the owning recipe (and, for step notes, the owning instruction) via {@link RecipeRepository} and
 * {@link InstructionRepository} before persisting, and coordinates with
 * {@link FavoriteRecipeRepository} for the auto-favorite side effect described on {@link #saveNote}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class PersonalNoteServiceImp implements PersonalNoteService{

    private final PersonalInstructionNoteRepository noteRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final FavoriteRecipeRepository favoriteRepository;
    private final InstructionRepository instructionRepository;

    /**
     * Saves or updates a personal private note for a recipe or specific instruction step. As a
     * side effect, also bookmarks the recipe to the user's favorites if it isn't already
     * favorited, since writing a note is a strong signal of interest; this is one-directional —
     * later deleting the note that triggered it does not remove the favorite.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param request note creation or update request DTO
     * @param userEmail user email address
     * @throws ResourceNotFoundException if the user or recipe cannot be found, or if
     *         {@code request.instructionId()} doesn't identify a step belonging to that recipe
     */
    @Transactional
    public void saveNote(NoteRequestDTO request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        Recipe recipe = recipeRepository.findById(request.recipeId().toString())
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", request.recipeId().toString()));

        String instructionId = request.instructionId() != null ? request.instructionId().toString() : null;
        if (instructionId != null && !instructionRepository.existsByIdAndRecipeId(instructionId, recipe.getId())) {
            throw new ResourceNotFoundException("Instruction", instructionId);
        }

        Optional<PersonalInstructionNote> existingNote = instructionId == null
                ? noteRepository.findByUserIdAndRecipeIdAndInstructionIdIsNull(user.getId(), request.recipeId().toString())
                : noteRepository.findByUserIdAndRecipeIdAndInstructionId(user.getId(), request.recipeId().toString(), instructionId);

        PersonalInstructionNote note = existingNote.orElse(PersonalInstructionNote.builder()
                .user(user)
                .recipe(recipe)
                .build());

        note.setNote(request.note());
        note.setInstructionId(instructionId);

        noteRepository.save(note);

        if (!favoriteRepository.existsByUserIdAndRecipeId(user.getId(), recipe.getId())) {
            FavoriteRecipe favorite = FavoriteRecipe.builder()
                    .user(user)
                    .recipe(recipe)
                    .build();
            favoriteRepository.save(favorite);
        }
    }

    /**
     * Retrieves general recipe-level personal note (where instruction IS NULL).
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param recipeId target recipe ID
     * @param userEmail user email address
     * @return NoteResponse DTO or null if no note attached
     */
    @Transactional(readOnly = true)
    public NoteResponse getNote(String recipeId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        return noteRepository.findByUserIdAndRecipeIdAndInstructionIdIsNull(user.getId(), recipeId)
                .map(PersonalNoteServiceImp::toResponse)
                .orElse(null);
    }

    /**
     * Retrieves all personal notes created by user for a recipe (general + step-specific notes).
     *
     * Complexity:
     * Time: O(N) where N is user note count for recipe
     * Space: O(N)
     *
     * @param recipeId target recipe ID
     * @param userEmail user email address
     * @param page page number
     * @param size page size
     * @return PagedResponse of NoteResponse DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<NoteResponse> getNotesForRecipe(String recipeId, String userEmail, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        Page<PersonalInstructionNote> notesPage = noteRepository.findAllByUserIdAndRecipeId(
                user.getId(), recipeId, PageRequest.of(page, size));

        return PagedResponseMapper.toPagedResponse(notesPage, PersonalNoteServiceImp::toResponse);
    }

    private static NoteResponse toResponse(PersonalInstructionNote n) {
        return new NoteResponse(
                n.getId(),
                n.getRecipe().getId(),
                n.getInstruction() != null ? n.getInstruction().getId() : null,
                n.getNote());
    }

    /**
     * Deletes a personal note following author verification.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param noteId target note ID
     * @param userEmail user email address
     */
    @Transactional
    public void deleteNote(String noteId, String userEmail) {
        PersonalInstructionNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", noteId));

        if (!note.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedActionException("You are not allowed to delete this note.");
        }

        noteRepository.delete(note);
    }
}
