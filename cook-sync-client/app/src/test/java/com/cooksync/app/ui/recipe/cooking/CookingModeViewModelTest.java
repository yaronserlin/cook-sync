package com.cooksync.app.ui.recipe.cooking;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.note.NoteResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for the private-note loading on {@link CookingModeViewModel}, mirroring the same
 * repository-delegation coverage {@code RecipeDetailViewModelTest} has for
 * {@code RecipeDetailViewModel}. The step-navigation and countdown-timer logic elsewhere in
 * this ViewModel is out of scope here.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
public class CookingModeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RecipeRepository repository;
    private CookingModeViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(RecipeRepository.class);
        viewModel = new CookingModeViewModel(repository);
    }

    @Test
    public void loadNotes_publishesNotesFromRepository() {
        NoteResponse recipeNote = new NoteResponse("n1", "recipe-1", null, "Great recipe");
        NoteResponse stepNote = new NoteResponse("n2", "recipe-1", "step-1", "Whisk gently");
        doAnswer(ApiResultAnswers.success(List.of(recipeNote, stepNote)))
                .when(repository).getAllPersonalNotes(eq("recipe-1"), any());

        viewModel.loadNotes("recipe-1");

        ApiResult<List<NoteResponse>> result = viewModel.getNotesResult().getValue();
        assertTrue(result instanceof ApiResult.Success<List<NoteResponse>>);
        assertEquals(List.of(recipeNote, stepNote), ((ApiResult.Success<List<NoteResponse>>) result).getData());
    }

    @Test
    public void loadNotes_publishesErrorResult_whenRepositoryFails() {
        doAnswer(ApiResultAnswers.<List<NoteResponse>>error("network error"))
                .when(repository).getAllPersonalNotes(eq("recipe-1"), any());

        viewModel.loadNotes("recipe-1");

        ApiResult<List<NoteResponse>> result = viewModel.getNotesResult().getValue();
        assertTrue(result instanceof ApiResult.Error<List<NoteResponse>>);
    }
}
