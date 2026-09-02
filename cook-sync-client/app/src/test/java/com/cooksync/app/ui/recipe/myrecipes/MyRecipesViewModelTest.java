package com.cooksync.app.ui.recipe.myrecipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.data.repository.TagRepository;
import com.cooksync.app.data.service.RecipePublishManager;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.List;

/**
 * Unit tests for {@link MyRecipesViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
public class MyRecipesViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RecipeRepository recipeRepository;
    private TagRepository tagRepository;
    private MockedStatic<RecipePublishManager> publishManagerStatic;
    private MyRecipesViewModel viewModel;

    private final RecipePreviewResponse recipeOne = new RecipePreviewResponse("recipe-1", "Gordon", "Beef Wellington",
            "A classic", "HARD", "PUBLIC", 30, 60, 4, 4.5, "2026-01-01", List.of(), null, false, null, false);

    private final RecipePreviewResponse recipeTwo = new RecipePreviewResponse("recipe-2", "Gordon", "Boeuf Bourguignon",
            "Also classic", "MEDIUM", "PRIVATE", 20, 90, 2, 4.0, "2026-01-02", List.of(), null, false, null, false);

    @Before
    public void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        tagRepository = mock(TagRepository.class);

        RecipePublishManager publishManager = mock(RecipePublishManager.class);
        when(publishManager.getRecipePublishedEvent()).thenReturn(new MutableLiveData<>());
        publishManagerStatic = mockStatic(RecipePublishManager.class);
        publishManagerStatic.when(RecipePublishManager::getInstance).thenReturn(publishManager);

        viewModel = new MyRecipesViewModel(recipeRepository, tagRepository);
    }

    @After
    public void tearDown() {
        publishManagerStatic.close();
    }

    @Test
    public void loadMyRecipes_publishesFetchedLibrary() {
        stubMyRecipes(List.of(recipeOne, recipeTwo));

        viewModel.loadMyRecipes();

        // Default sort is "Newest" (createdAt descending), so recipeTwo (2026-01-02) sorts
        // ahead of recipeOne (2026-01-01).
        assertEquals(List.of(recipeTwo, recipeOne), successRecipes());
        assertTrue(viewModel.hasAnyRecipes());
    }

    @Test
    public void hasAnyRecipes_falseBeforeAnyLoad() {
        assertFalse(viewModel.hasAnyRecipes());
    }

    @Test
    public void getPublishedCount_countsPublicRecipes_acrossWholeLibrary_ignoringActiveFilter() {
        stubMyRecipes(List.of(recipeOne, recipeTwo));
        viewModel.loadMyRecipes();

        viewModel.setVisibilityFilter("PRIVATE");

        assertEquals(1, viewModel.getPublishedCount());
    }

    @Test
    public void getTotalCount_andGetWithPrivateNotesCount_reflectWholeLibrary_ignoringActiveFilter() {
        RecipePreviewResponse withNote = new RecipePreviewResponse("recipe-3", "Gordon", "Coq au Vin",
                "Classic too", "HARD", "PUBLIC", 25, 45, 1, 5.0, "2026-01-03", List.of(), null, true, "Double the wine", false);
        stubMyRecipes(List.of(recipeOne, recipeTwo, withNote));
        viewModel.loadMyRecipes();

        viewModel.setVisibilityFilter("PRIVATE");

        assertEquals(3, viewModel.getTotalCount());
        assertEquals(1, viewModel.getWithPrivateNotesCount());
    }

    @Test
    public void search_filtersLoadedLibrary_byTitle_withoutNewFetch() {
        stubMyRecipes(List.of(recipeOne, recipeTwo));
        viewModel.loadMyRecipes();

        viewModel.search("Wellington");

        assertEquals(List.of(recipeOne), successRecipes());
        verify(recipeRepository, org.mockito.Mockito.times(1)).getMyRecipes(any());
    }

    @Test
    public void setVisibilityFilter_narrowsToMatchingVisibility() {
        stubMyRecipes(List.of(recipeOne, recipeTwo));
        viewModel.loadMyRecipes();

        viewModel.setVisibilityFilter("PUBLIC");

        assertEquals(List.of(recipeOne), successRecipes());
    }

    @Test
    public void applyFilters_difficultyFilter_narrowsResults() {
        stubMyRecipes(List.of(recipeOne, recipeTwo));
        viewModel.loadMyRecipes();

        viewModel.applyFilters("Newest", "HARD", List.of(), null, null);

        assertEquals(List.of(recipeOne), successRecipes());
    }

    @Test
    public void deleteRecipe_onSuccess_removesFromList_andPublishesSuccess() {
        stubMyRecipes(List.of(recipeOne, recipeTwo));
        viewModel.loadMyRecipes();
        doAnswer(ApiResultAnswers.success((Void) null)).when(recipeRepository).deleteRecipe(eq("recipe-1"), any());

        viewModel.deleteRecipe(recipeOne);

        assertEquals(List.of(recipeTwo), successRecipes());
        assertTrue(viewModel.getDeleteResult().getValue() instanceof ApiResult.Success<Void>);
    }

    @Test
    public void deleteRecipe_onError_keepsRecipeInList_andPublishesError() {
        stubMyRecipes(List.of(recipeOne));
        viewModel.loadMyRecipes();
        doAnswer(ApiResultAnswers.<Void>error("Server unavailable"))
                .when(recipeRepository).deleteRecipe(eq("recipe-1"), any());

        viewModel.deleteRecipe(recipeOne);

        assertEquals(List.of(recipeOne), successRecipes());
        assertTrue(viewModel.getDeleteResult().getValue() instanceof ApiResult.Error<Void>);
    }

    @Test
    public void deleteRecipe_cancelsPendingVisibilityToggle_forSameRecipe() {
        stubMyRecipes(List.of(recipeOne));
        viewModel.loadMyRecipes();
        doAnswer(ApiResultAnswers.success((Void) null)).when(recipeRepository).deleteRecipe(eq("recipe-1"), any());

        viewModel.toggleVisibility(recipeOne);
        viewModel.deleteRecipe(recipeOne);

        // The deferred visibility PATCH is scheduled several seconds out; deleting immediately
        // afterward must cancel it, or it would eventually fire against an already-deleted
        // recipe and surface a spurious error.
        verify(recipeRepository, never()).updateRecipeVisibility(any(), any(), any());
    }

    @Test
    public void toggleVisibility_flipsVisibilityImmediately_beforeServerResponds() {
        stubMyRecipes(List.of(recipeOne));
        viewModel.loadMyRecipes();

        viewModel.toggleVisibility(recipeOne);

        List<RecipePreviewResponse> displayed = successRecipes();
        assertEquals(1, displayed.size());
        assertEquals("PRIVATE", displayed.get(0).visibility());
        verify(recipeRepository, never()).updateRecipeVisibility(any(), any(), any());
    }

    @Test
    public void undoToggleVisibility_beforeWindowElapses_restoresOriginal_andNeverCallsRepository() {
        stubMyRecipes(List.of(recipeOne));
        viewModel.loadMyRecipes();
        viewModel.toggleVisibility(recipeOne);

        viewModel.undoToggleVisibility(recipeOne);

        List<RecipePreviewResponse> displayed = successRecipes();
        assertEquals("PUBLIC", displayed.get(0).visibility());
        verify(recipeRepository, never()).updateRecipeVisibility(any(), any(), any());
    }

    @Test
    public void onCleared_flushesPendingVisibilityToggle_callsRepositoryImmediately() {
        stubMyRecipes(List.of(recipeOne));
        viewModel.loadMyRecipes();
        viewModel.toggleVisibility(recipeOne);
        RecipeResponse response = new RecipeResponse(null, null, null, null, null, 0, 0, 0, 0, null, null, null, null, null, null, null, null, null, false);
        doAnswer(ApiResultAnswers.success(response))
                .when(recipeRepository).updateRecipeVisibility(eq("recipe-1"), eq("PRIVATE"), any());

        viewModel.onCleared();

        verify(recipeRepository).updateRecipeVisibility(eq("recipe-1"), eq("PRIVATE"), any());
    }

    @Test
    public void onCleared_flushesPendingVisibilityToggle_serverError_rollsBackVisibility_andPublishesError() {
        stubMyRecipes(List.of(recipeOne));
        viewModel.loadMyRecipes();
        viewModel.toggleVisibility(recipeOne);
        doAnswer(ApiResultAnswers.<RecipeResponse>error("Server unavailable"))
                .when(recipeRepository).updateRecipeVisibility(eq("recipe-1"), eq("PRIVATE"), any());

        viewModel.onCleared();

        verify(recipeRepository).updateRecipeVisibility(eq("recipe-1"), eq("PRIVATE"), any());
        assertEquals("PUBLIC", successRecipes().get(0).visibility());
        assertTrue(viewModel.getVisibilityResult().getValue() instanceof ApiResult.Error<RecipeResponse>);
    }

    @Test
    public void loadMyRecipes_publishesError_whenRepositoryFails() {
        doAnswer(ApiResultAnswers.<List<RecipePreviewResponse>>error("Server unavailable"))
                .when(recipeRepository).getMyRecipes(any());

        viewModel.loadMyRecipes();

        assertTrue(viewModel.getRecipesResult().getValue() instanceof ApiResult.Error<List<RecipePreviewResponse>>);
        assertFalse(viewModel.hasAnyRecipes());
    }

    private void stubMyRecipes(List<RecipePreviewResponse> recipes) {
        doAnswer(ApiResultAnswers.success(recipes)).when(recipeRepository).getMyRecipes(any());
    }

    @SuppressWarnings("unchecked")
    private List<RecipePreviewResponse> successRecipes() {
        ApiResult<List<RecipePreviewResponse>> result = viewModel.getRecipesResult().getValue();
        return ((ApiResult.Success<List<RecipePreviewResponse>>) result).getData();
    }
}
