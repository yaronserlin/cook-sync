package com.cooksync.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.data.repository.TagRepository;
import com.cooksync.app.data.service.RecipePublishManager;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.domain.Event;
import com.cooksync.app.domain.FeedState;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;

/**
 * Unit tests for {@link HomeViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
public class HomeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RecipeRepository recipeRepository;
    private TagRepository tagRepository;
    private MockedStatic<RecipePublishManager> publishManagerStatic;
    private MutableLiveData<Event<RecipeResponse>> recipePublishedEvent;
    private HomeViewModel viewModel;

    private final RecipePreviewResponse recipeOne = new RecipePreviewResponse("recipe-1", "Gordon", "Beef Wellington",
            "A classic", "HARD", "PUBLIC", 30, 60, 4, 4.5, "2026-01-01", List.of(), null, false, null);

    private final RecipePreviewResponse recipeTwo = new RecipePreviewResponse("recipe-2", "Julia", "Boeuf Bourguignon",
            "Also classic", "MEDIUM", "PUBLIC", 20, 90, 2, 4.0, "2026-01-02", List.of(), null, false, null);

    @Before
    public void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        tagRepository = mock(TagRepository.class);

        RecipePublishManager publishManager = mock(RecipePublishManager.class);
        recipePublishedEvent = new MutableLiveData<>();
        when(publishManager.getRecipePublishedEvent()).thenReturn(recipePublishedEvent);
        publishManagerStatic = mockStatic(RecipePublishManager.class);
        publishManagerStatic.when(RecipePublishManager::getInstance).thenReturn(publishManager);

        viewModel = new HomeViewModel(recipeRepository, tagRepository);
    }

    @After
    public void tearDown() {
        publishManagerStatic.close();
    }

    @Test
    public void loadInitialFeed_publishesSuccessState_fromGeneralBrowseFeed() {
        stubPublicFeed(0, page(List.of(recipeOne), true));

        viewModel.loadInitialFeed();

        FeedState.Success state = (FeedState.Success) viewModel.getFeedState().getValue();
        assertEquals(List.of(recipeOne), state.getRecipes());
        assertFalse(state.hasMore());
        verify(recipeRepository, never()).getRecipesByTag(any(), anyInt(), anyInt(), any());
    }

    @Test
    public void loadNextPage_fetchesSubsequentPage_andAppendsResults_whenNotLastPage() {
        stubPublicFeed(0, page(List.of(recipeOne), false));
        stubPublicFeed(1, page(List.of(recipeTwo), true));
        viewModel.loadInitialFeed();

        viewModel.loadNextPage();

        // Default sort is "Newest" (createdAt descending), so recipeTwo (2026-01-02) sorts
        // ahead of recipeOne (2026-01-01) once both pages are merged.
        FeedState.Success state = (FeedState.Success) viewModel.getFeedState().getValue();
        assertEquals(List.of(recipeTwo, recipeOne), state.getRecipes());
        verify(recipeRepository).getPublicFeed(eq(1), eq(10), any());
    }

    @Test
    public void loadNextPage_noOpWhenAlreadyOnLastPage() {
        stubPublicFeed(0, page(List.of(recipeOne), true));
        viewModel.loadInitialFeed();

        viewModel.loadNextPage();

        verify(recipeRepository, times(1)).getPublicFeed(anyInt(), anyInt(), any());
    }

    @Test
    public void toggleTag_singleSelection_routesToTagFilteredEndpoint() {
        doAnswer(ApiResultAnswers.success(page(List.of(recipeOne), true)))
                .when(recipeRepository).getRecipesByTag(eq("Vegan"), eq(0), eq(10), any());

        viewModel.toggleTag("Vegan");

        assertEquals(Set.of("Vegan"), viewModel.getSelectedTags());
        verify(recipeRepository).getRecipesByTag(eq("Vegan"), eq(0), eq(10), any());
        verify(recipeRepository, never()).getPublicFeed(anyInt(), anyInt(), any());
    }

    @Test
    public void toggleTag_secondSelection_fallsBackToGeneralFeed() {
        doAnswer(ApiResultAnswers.success(page(List.of(recipeOne), true)))
                .when(recipeRepository).getRecipesByTag(eq("Vegan"), eq(0), eq(10), any());
        stubPublicFeed(0, page(List.of(recipeOne, recipeTwo), true));

        viewModel.toggleTag("Vegan");
        viewModel.toggleTag("Dessert");

        assertEquals(Set.of("Vegan", "Dessert"), viewModel.getSelectedTags());
        verify(recipeRepository).getRecipesByTag(eq("Vegan"), eq(0), eq(10), any());
        verify(recipeRepository).getPublicFeed(eq(0), eq(10), any());
    }

    @Test
    public void clearTags_noOpWhenNoTagsSelected() {
        viewModel.clearTags();

        verifyNoInteractions(recipeRepository);
    }

    @Test
    public void toggleFavorite_addingUnfavorited_updatesResultOptimistically_andCallsAddFavoriteImmediately() {
        stubPublicFeed(0, page(List.of(recipeOne), true));
        viewModel.loadInitialFeed();
        doAnswer(ApiResultAnswers.success((Void) null)).when(recipeRepository).addFavorite(eq("recipe-1"), any());

        viewModel.toggleFavorite("recipe-1");

        List<RecipePreviewResponse> favorites = successFavorites();
        assertEquals(List.of(recipeOne), favorites);
        verify(recipeRepository).addFavorite(eq("recipe-1"), any());
    }

    @Test
    public void toggleFavorite_removingFavorited_updatesImmediately_butDefersRepositoryCall() {
        loadFavoritesContaining(recipeOne);

        viewModel.toggleFavorite("recipe-1");

        assertTrue(successFavorites().isEmpty());
        verify(recipeRepository, never()).removeFavorite(eq("recipe-1"), any());
    }

    @Test
    public void undoRemoveFavorite_beforeWindowElapses_reloadsFavorites_andNeverCallsRemove() {
        loadFavoritesContaining(recipeOne);
        viewModel.toggleFavorite("recipe-1");

        viewModel.undoRemoveFavorite("recipe-1");

        assertEquals(List.of(recipeOne), successFavorites());
        verify(recipeRepository, never()).removeFavorite(eq("recipe-1"), any());
    }

    @Test
    public void onCleared_flushesPendingFavoriteRemoval_callsRepositoryImmediately() {
        loadFavoritesContaining(recipeOne);
        viewModel.toggleFavorite("recipe-1");
        doAnswer(ApiResultAnswers.success((Void) null)).when(recipeRepository).removeFavorite(eq("recipe-1"), any());

        viewModel.onCleared();

        verify(recipeRepository).removeFavorite(eq("recipe-1"), any());
    }

    @Test
    public void recipePublishedEvent_firingWithNewRecipe_reloadsFeed() {
        stubPublicFeed(0, page(List.of(recipeOne), true));

        recipePublishedEvent.setValue(new Event<>(
                new RecipeResponse(null, null, null, null, null, 0, 0, 0, 0, null, null, null, null, null, null, null, null, null)));

        verify(recipeRepository).getPublicFeed(eq(0), eq(10), any());
    }

    private void loadFavoritesContaining(RecipePreviewResponse... recipes) {
        doAnswer(ApiResultAnswers.success(List.of(recipes))).when(recipeRepository).getFavorites(any());
        viewModel.loadFavorites();
    }

    private void stubPublicFeed(int pageNumber, PagedResponse<RecipePreviewResponse> response) {
        doAnswer(ApiResultAnswers.success(response)).when(recipeRepository).getPublicFeed(eq(pageNumber), eq(10), any());
    }

    private PagedResponse<RecipePreviewResponse> page(List<RecipePreviewResponse> content, boolean last) {
        return new PagedResponse<>(content, 0, 10, content.size(), 1, last);
    }

    @SuppressWarnings("unchecked")
    private List<RecipePreviewResponse> successFavorites() {
        ApiResult<List<RecipePreviewResponse>> result = viewModel.getFavoritesResult().getValue();
        return ((ApiResult.Success<List<RecipePreviewResponse>>) result).getData();
    }
}
