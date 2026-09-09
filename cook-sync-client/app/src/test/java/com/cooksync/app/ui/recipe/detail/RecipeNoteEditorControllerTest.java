package com.cooksync.app.ui.recipe.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.cooksync.app.data.repository.RecipeRepository;
import com.dtos.response.note.NoteResponse;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link RecipeNoteEditorController}'s note-list bookkeeping, recipe/step-note
 * lookup delegation, and duplicate-commit guard.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public class RecipeNoteEditorControllerTest {

    private RecipeNoteEditorController controller;

    @Before
    public void setUp() {
        RecipeDetailViewModel viewModel = new RecipeDetailViewModel(mock(RecipeRepository.class));
        controller = new RecipeNoteEditorController(viewModel);
    }

    @Test
    public void setNotes_replacesPreviouslyStoredNotes() {
        NoteResponse first = new NoteResponse("n1", "recipe-1", null, "First");
        NoteResponse second = new NoteResponse("n2", "recipe-1", null, "Second");
        controller.setNotes(List.of(first));

        controller.setNotes(List.of(second));

        assertEquals(List.of(second), controller.getNotes());
    }

    @Test
    public void setNotes_toleratesNullList() {
        controller.setNotes(null);

        assertTrue(controller.getNotes().isEmpty());
    }

    @Test
    public void getRecipeNote_returnsTheNoteWithNoInstructionId() {
        NoteResponse recipeNote = new NoteResponse("n1", "recipe-1", null, "Great recipe");
        NoteResponse stepNote = new NoteResponse("n2", "recipe-1", "step-1", "Reduce salt");
        controller.setNotes(List.of(stepNote, recipeNote));

        assertEquals(recipeNote, controller.getRecipeNote());
    }

    @Test
    public void getRecipeNote_returnsNull_whenOnlyStepNotesExist() {
        NoteResponse stepNote = new NoteResponse("n2", "recipe-1", "step-1", "Reduce salt");
        controller.setNotes(List.of(stepNote));

        assertNull(controller.getRecipeNote());
    }

    @Test
    public void getStepNote_returnsTheMatchingStepNote() {
        NoteResponse stepNote = new NoteResponse("n2", "recipe-1", "step-1", "Reduce salt");
        controller.setNotes(List.of(stepNote));

        assertEquals(stepNote, controller.getStepNote("step-1"));
        assertNull(controller.getStepNote("step-2"));
    }

    @Test
    public void tryCommit_succeedsOnceThenBlocksUntilEditorReopens() {
        controller.openEditor();

        assertTrue(controller.tryCommit());
        assertFalse(controller.tryCommit());

        controller.openEditor();
        assertTrue(controller.tryCommit());
    }
}
