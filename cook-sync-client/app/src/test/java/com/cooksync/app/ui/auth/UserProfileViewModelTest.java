package com.cooksync.app.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.AuthRepository;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.user.PublicUserProfileResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link UserProfileViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
public class UserProfileViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AuthRepository authRepository;
    private RecipeRepository recipeRepository;
    private UserProfileViewModel viewModel;

    @Before
    public void setUp() {
        authRepository = mock(AuthRepository.class);
        recipeRepository = mock(RecipeRepository.class);
        viewModel = new UserProfileViewModel(authRepository, recipeRepository);
    }

    @Test
    public void loadProfile_publishesProfile_thenLoadsRecipesAndFavorites_whenBothPublic() {
        PublicUserProfileResponse profile = new PublicUserProfileResponse(
                "user-2", "Jane", "Smith", null, "Tel Aviv", "Home cook.", true, true);
        doAnswer(ApiResultAnswers.success(profile)).when(authRepository).getUserProfile(eq("user-2"), any());
        RecipePreviewResponse recipe = new RecipePreviewResponse("recipe-1", "Jane", "Soup",
                "desc", "EASY", "PUBLIC", 10, 20, 0, null, "2026-01-01", List.of(), null, false, null, false);
        doAnswer(ApiResultAnswers.success(List.of(recipe))).when(recipeRepository).getPublicRecipesForUser(eq("user-2"), any());
        doAnswer(ApiResultAnswers.success(List.of(recipe))).when(recipeRepository).getPublicFavoritesForUser(eq("user-2"), any());

        viewModel.loadProfile("user-2");

        assertEquals(profile, ((ApiResult.Success<PublicUserProfileResponse>) viewModel.getProfileResult().getValue()).getData());
        assertEquals(1, ((ApiResult.Success<List<RecipePreviewResponse>>) viewModel.getRecipesResult().getValue()).getData().size());
        assertEquals(1, ((ApiResult.Success<List<RecipePreviewResponse>>) viewModel.getFavoritesResult().getValue()).getData().size());
    }

    @Test
    public void loadProfile_skipsRecipesLoad_whenShowRecipesPubliclyFalse() {
        PublicUserProfileResponse profile = new PublicUserProfileResponse(
                "user-2", "Jane", "Smith", null, null, null, false, true);
        doAnswer(ApiResultAnswers.success(profile)).when(authRepository).getUserProfile(eq("user-2"), any());
        doAnswer(ApiResultAnswers.success(List.<RecipePreviewResponse>of())).when(recipeRepository).getPublicFavoritesForUser(eq("user-2"), any());

        viewModel.loadProfile("user-2");

        verify(recipeRepository, never()).getPublicRecipesForUser(eq("user-2"), any());
    }

    @Test
    public void loadProfile_skipsFavoritesLoad_whenShowFavoritesPubliclyFalse() {
        PublicUserProfileResponse profile = new PublicUserProfileResponse(
                "user-2", "Jane", "Smith", null, null, null, true, false);
        doAnswer(ApiResultAnswers.success(profile)).when(authRepository).getUserProfile(eq("user-2"), any());
        doAnswer(ApiResultAnswers.success(List.<RecipePreviewResponse>of())).when(recipeRepository).getPublicRecipesForUser(eq("user-2"), any());

        viewModel.loadProfile("user-2");

        verify(recipeRepository, never()).getPublicFavoritesForUser(eq("user-2"), any());
    }

    @Test
    public void loadProfile_publishesError_andSkipsBothLists_whenProfileFetchFails() {
        doAnswer(ApiResultAnswers.<PublicUserProfileResponse>error("network error"))
                .when(authRepository).getUserProfile(eq("user-2"), any());

        viewModel.loadProfile("user-2");

        assertTrue(viewModel.getProfileResult().getValue() instanceof ApiResult.Error<PublicUserProfileResponse>);
        verify(recipeRepository, never()).getPublicRecipesForUser(any(), any());
        verify(recipeRepository, never()).getPublicFavoritesForUser(any(), any());
    }
}
