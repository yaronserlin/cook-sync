package com.cooksync.app.ui.recipe.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.data.repository.TagRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.domain.FeedState;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.tags.TagResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Set;

/**
 * Unit tests for {@link SearchViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
public class SearchViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RecipeRepository recipeRepository;
    private TagRepository tagRepository;
    private SearchViewModel viewModel;

    private final RecipePreviewResponse recipeOne = new RecipePreviewResponse("recipe-1", "Gordon", "Beef Wellington",
            "A classic", "HARD", "PUBLIC", 30, 60, 4, 4.5, "2026-01-01", List.of(), null, false, null);

    private final RecipePreviewResponse recipeTwo = new RecipePreviewResponse("recipe-2", "Julia", "Boeuf Bourguignon",
            "Also classic", "MEDIUM", "PUBLIC", 20, 90, 2, 4.0, "2026-01-02", List.of(), null, false, null);

    @Before
    public void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        tagRepository = mock(TagRepository.class);
        viewModel = new SearchViewModel(recipeRepository, tagRepository);
    }

    @Test
    public void search_withBlankQuery_clearsResults_withoutCallingRepository() {
        viewModel.search(null);

        FeedState.Success state = (FeedState.Success) viewModel.getFeedState().getValue();
        assertTrue(state.getRecipes().isEmpty());
        assertFalse(state.hasMore());
        verifyNoInteractions(recipeRepository);
    }

    @Test
    public void search_withQuery_fetchesFirstPage_fromSearchEndpoint() {
        stubSearch("pasta", 0, page(List.of(recipeOne), true));

        viewModel.search("pasta");

        FeedState.Success state = (FeedState.Success) viewModel.getFeedState().getValue();
        assertEquals(List.of(recipeOne), state.getRecipes());
        assertFalse(state.hasMore());
        verify(recipeRepository, never()).getRecipesByTag(any(), any(Integer.class), any(Integer.class), any());
    }

    @Test
    public void search_thenBlankQuery_clearsResults_withoutIssuingNewFetch() {
        stubSearch("pasta", 0, page(List.of(recipeOne), true));
        viewModel.search("pasta");

        viewModel.search(null);

        FeedState.Success state = (FeedState.Success) viewModel.getFeedState().getValue();
        assertTrue(state.getRecipes().isEmpty());
        verify(recipeRepository, times(1)).searchRecipes(any(), any(Integer.class), any(Integer.class), any());
    }

    @Test
    public void searchByTag_fetchesFromTagEndpoint_andSetsSingleSelectedTag() {
        stubTagBrowse("Vegan", 0, page(List.of(recipeOne), true));

        viewModel.searchByTag("Vegan");

        assertEquals(Set.of("Vegan"), viewModel.getSelectedTags());
        verify(recipeRepository).getRecipesByTag(eq("Vegan"), eq(0), eq(10), any());
        verify(recipeRepository, never()).searchRecipes(any(), any(Integer.class), any(Integer.class), any());
    }

    @Test
    public void searchByTag_afterPriorTagSelected_replacesSelection_ratherThanAddingToIt() {
        viewModel.applyFilters("Newest", null, List.of("Dessert"), null, null);
        stubTagBrowse("Vegan", 0, page(List.of(recipeOne), true));

        viewModel.searchByTag("Vegan");

        assertEquals(Set.of("Vegan"), viewModel.getSelectedTags());
    }

    @Test
    public void search_afterTagBrowseActive_switchesBackToKeywordEndpoint() {
        stubTagBrowse("Vegan", 0, page(List.of(recipeOne), true));
        viewModel.searchByTag("Vegan");
        stubSearch("pasta", 0, page(List.of(recipeTwo), true));

        viewModel.search("pasta");

        verify(recipeRepository).searchRecipes(eq("pasta"), eq(0), eq(10), any());
    }

    @Test
    public void loadNextPage_whenKeywordSearchActive_fetchesNextPage_andAppendsResults() {
        stubSearch("pasta", 0, page(List.of(recipeOne), false));
        stubSearch("pasta", 1, page(List.of(recipeTwo), true));
        viewModel.search("pasta");

        viewModel.loadNextPage();

        // Default sort is "Newest" (createdAt descending), so recipeTwo (2026-01-02) sorts
        // ahead of recipeOne (2026-01-01) once both pages are merged.
        FeedState.Success state = (FeedState.Success) viewModel.getFeedState().getValue();
        assertEquals(List.of(recipeTwo, recipeOne), state.getRecipes());
        verify(recipeRepository).searchRecipes(eq("pasta"), eq(1), eq(10), any());
    }

    @Test
    public void loadNextPage_noOpWhenAlreadyOnLastPage() {
        stubSearch("pasta", 0, page(List.of(recipeOne), true));
        viewModel.search("pasta");

        viewModel.loadNextPage();

        verify(recipeRepository, times(1)).searchRecipes(any(), any(Integer.class), any(Integer.class), any());
    }

    @Test
    public void loadTags_populatesTagsResult() {
        List<TagResponse> tags = List.of(new TagResponse("t1", "Vegan", null, null),
                new TagResponse("t2", "Vegetarian", null, null));
        doAnswer(ApiResultAnswers.success(tags)).when(tagRepository).getAllTags(any());

        viewModel.loadTags();

        @SuppressWarnings("unchecked")
        ApiResult.Success<List<TagResponse>> result =
                (ApiResult.Success<List<TagResponse>>) viewModel.getTagsResult().getValue();
        assertEquals(tags, result.getData());
    }

    @Test
    public void getMatchingTagSuggestions_beforeTagsLoaded_returnsEmpty() {
        assertTrue(viewModel.getMatchingTagSuggestions("veg").isEmpty());
    }

    @Test
    public void getMatchingTagSuggestions_matchesCaseInsensitiveSubstring_afterTagsLoaded() {
        List<TagResponse> tags = List.of(new TagResponse("t1", "Vegan", null, null),
                new TagResponse("t2", "Vegetarian", null, null),
                new TagResponse("t3", "Dessert", null, null));
        doAnswer(ApiResultAnswers.success(tags)).when(tagRepository).getAllTags(any());
        viewModel.loadTags();

        assertEquals(List.of("Vegan", "Vegetarian"), viewModel.getMatchingTagSuggestions("VEG"));
    }

    @Test
    public void getMatchingTagSuggestions_blankQuery_returnsEmpty() {
        assertTrue(viewModel.getMatchingTagSuggestions("").isEmpty());
        assertTrue(viewModel.getMatchingTagSuggestions(null).isEmpty());
    }

    @Test
    public void clearAllFilters_reFiltersLoadedResults_withoutIssuingNewFetch_andKeepsQuery() {
        stubSearch("pasta", 0, page(List.of(recipeOne, recipeTwo), true));
        viewModel.search("pasta");
        viewModel.applyFilters("Newest", "HARD", List.of(), null, null);

        viewModel.clearAllFilters();

        FeedState.Success state = (FeedState.Success) viewModel.getFeedState().getValue();
        assertEquals(List.of(recipeTwo, recipeOne), state.getRecipes());
        assertEquals("pasta", viewModel.getCurrentQuery());
        verify(recipeRepository, times(1)).searchRecipes(any(), any(Integer.class), any(Integer.class), any());
    }

    private void stubSearch(String query, int pageNumber, PagedResponse<RecipePreviewResponse> response) {
        doAnswer(ApiResultAnswers.success(response))
                .when(recipeRepository).searchRecipes(eq(query), eq(pageNumber), eq(10), any());
    }

    private void stubTagBrowse(String tagName, int pageNumber, PagedResponse<RecipePreviewResponse> response) {
        doAnswer(ApiResultAnswers.success(response))
                .when(recipeRepository).getRecipesByTag(eq(tagName), eq(pageNumber), eq(10), any());
    }

    private PagedResponse<RecipePreviewResponse> page(List<RecipePreviewResponse> content, boolean last) {
        return new PagedResponse<>(content, 0, 10, content.size(), 1, last);
    }
}
