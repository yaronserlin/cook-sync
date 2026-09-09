package com.cooksync.app.ui.recipe.detail;

import com.cooksync.app.util.CommitOnceGuard;
import com.dtos.response.note.NoteResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns {@link RecipeDetailActivity}'s note-editing state: the recipe's currently loaded notes
 * (recipe-wide plus per-instruction-step) and the {@link CommitOnceGuard} that prevents a
 * duplicate save when both an explicit save tap and the resulting focus-loss fire for the same
 * user gesture. Deliberately free of any Android View dependency so it can be unit tested on the
 * plain JVM; {@link RecipeDetailActivity} owns one instance and delegates every note-editing
 * state transition to it, keeping view-binding code and state logic separate.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class RecipeNoteEditorController {

    private final RecipeDetailViewModel viewModel;
    private final List<NoteResponse> notes = new ArrayList<>();
    private final CommitOnceGuard guard = new CommitOnceGuard();

    /**
     * @param viewModel supplies the pure {@link RecipeDetailViewModel#findRecipeNote} and
     *                  {@link RecipeDetailViewModel#findStepNote} lookups this controller
     *                  delegates to
     */
    RecipeNoteEditorController(RecipeDetailViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Replaces the currently loaded notes, e.g. after a fresh load or a save/delete round-trip.
     *
     * @param newNotes every note on the recipe (recipe-wide plus per-step), or {@code null}
     */
    void setNotes(List<NoteResponse> newNotes) {
        notes.clear();
        if (newNotes != null) {
            notes.addAll(newNotes);
        }
    }

    /**
     * @return every currently loaded note on the recipe
     */
    List<NoteResponse> getNotes() {
        return notes;
    }

    /**
     * @return the recipe-wide note, or {@code null} if none exists
     */
    NoteResponse getRecipeNote() {
        return viewModel.findRecipeNote(notes);
    }

    /**
     * @param instructionId the instruction step to find the note for
     * @return the step's note, or {@code null} if none exists
     */
    NoteResponse getStepNote(String instructionId) {
        return viewModel.findStepNote(notes, instructionId);
    }

    /**
     * Resets the commit guard when the note editor opens, so the next {@link #tryCommit()}
     * succeeds regardless of a previous edit's commit history.
     */
    void openEditor() {
        guard.reset();
    }

    /**
     * Attempts to commit the currently open edit, succeeding only the first time this is called
     * since {@link #openEditor()} was last invoked.
     *
     * @return {@code true} if this call may proceed with the commit; {@code false} if a commit
     *         already went through for the current edit
     */
    boolean tryCommit() {
        return guard.tryCommit();
    }
}
