package com.cooksync.app.ui.recipe.favorites;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.data.repository.TagRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.recipe.RecipePreviewResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link FavoritesViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
public class FavoritesViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RecipeRepository repository;
    private TagRepository tagRepository;
    private FavoritesViewModel viewModel;

    private final RecipePreviewResponse annotatedRecipe = new RecipePreviewResponse("recipe-1", "Gordon", "Beef Wellington",
            "A classic", "HARD", "PUBLIC", 30, 60, 4, 4.5, "2026-01-01", List.of(), null, true, "Use less salt", false);

    private final RecipePreviewResponse plainRecipe = new RecipePreviewResponse("recipe-2", "Julia", "Boeuf Bourguignon",
            "Also classic", "MEDIUM", "PUBLIC", 20, 90, 2, 4.0, "2026-01-02", List.of(), null, false, null, false);

    @Before
    public void setUp() {
        repository = mock(RecipeRepository.class);
        tagRepository = mock(TagRepository.class);
        viewModel = new FavoritesViewModel(repository, tagRepository);
    }

    @Test
    public void loadFavorites_publishesSuccessResult_andComputesTotals() {
        loadTwoFavorites();

        ApiResult<List<RecipePreviewResponse>> result = viewModel.getDisplayedResult().getValue();
        assertTrue(result instanceof ApiResult.Success<List<RecipePreviewResponse>>);
        assertEquals(2, ((ApiResult.Success<List<RecipePreviewResponse>>) result).getData().size());
        assertEquals(2, viewModel.getTotalCount());
        assertEquals(1, viewModel.getWithNotesCount());
        assertTrue(viewModel.hasAnyFavorites());
    }

    @Test
    public void setOnlyWithNotes_filtersDownToAnnotatedFavoritesOnly() {
        loadTwoFavorites();

        viewModel.setOnlyWithNotes(true);

        List<RecipePreviewResponse> displayed = successData();
        assertEquals(1, displayed.size());
        assertEquals("recipe-1", displayed.get(0).id());
    }

    @Test
    public void setOnlyWithNotes_false_restoresTheFullList() {
        loadTwoFavorites();
        viewModel.setOnlyWithNotes(true);

        viewModel.setOnlyWithNotes(false);

        assertEquals(2, successData().size());
    }

    @Test
    public void removeFavorite_removesRowImmediately_andDoesNotCallRepositoryBeforeUndoWindow() {
        loadTwoFavorites();

        viewModel.removeFavorite("recipe-1");

        assertEquals(1, successData().size());
        assertEquals(1, viewModel.getTotalCount());
        verify(repository, never()).removeFavorite(eq("recipe-1"), any());
    }

    @Test
    public void undoRemoveFavorite_beforeWindowElapses_restoresRow_andNeverCallsRepository() {
        loadTwoFavorites();
        viewModel.removeFavorite("recipe-1");

        viewModel.undoRemoveFavorite(annotatedRecipe);

        assertEquals(2, successData().size());
        assertEquals(2, viewModel.getTotalCount());
        verify(repository, never()).removeFavorite(eq("recipe-1"), any());
    }

    @Test
    public void onCleared_flushesPendingRemoval_callsRepositoryImmediately() {
        loadTwoFavorites();
        doAnswer(ApiResultAnswers.<Void>success(null)).when(repository).removeFavorite(eq("recipe-1"), any());

        viewModel.removeFavorite("recipe-1");
        viewModel.onCleared();

        verify(repository).removeFavorite(eq("recipe-1"), any());
    }

    @Test
    public void loadFavorites_publishesErrorResult_whenRepositoryFails() {
        doAnswer(ApiResultAnswers.<List<RecipePreviewResponse>>error("network error")).when(repository).getFavorites(any());

        viewModel.loadFavorites();

        ApiResult<List<RecipePreviewResponse>> result = viewModel.getDisplayedResult().getValue();
        assertTrue(result instanceof ApiResult.Error<List<RecipePreviewResponse>>);
        assertFalse(viewModel.hasAnyFavorites());
    }

    private void loadTwoFavorites() {
        doAnswer(ApiResultAnswers.success(List.of(annotatedRecipe, plainRecipe))).when(repository).getFavorites(any());
        viewModel.loadFavorites();
    }

    @SuppressWarnings("unchecked")
    private List<RecipePreviewResponse> successData() {
        ApiResult<List<RecipePreviewResponse>> result = viewModel.getDisplayedResult().getValue();
        return ((ApiResult.Success<List<RecipePreviewResponse>>) result).getData();
    }
}
