package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.request.note.NoteRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.dtos.request.review.ReportReviewRequestDTO;
import com.dtos.request.review.ReviewRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.note.NoteResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;

import java.util.List;
import java.util.UUID;

/**
 * Concrete implementation of {@link RecipeRepository} that delegates calls to the remote
 * {@link ApiService} and manages execution on a background thread pool (inherited from
 * {@link BaseRepository}).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public class RecipeRepositoryImp extends BaseRepository implements RecipeRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public RecipeRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPublicFeed(int page, int size, MutableLiveData<ApiResult<PagedResponse<RecipePreviewResponse>>> resultTarget) {
        executeAsync(apiService.getPublicFeed(page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void searchRecipes(String query, int page, int size, MutableLiveData<ApiResult<PagedResponse<RecipePreviewResponse>>> resultTarget) {
        executeAsync(apiService.searchRecipes(query, null, null, page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getRecipesByTag(String tagName, int page, int size, MutableLiveData<ApiResult<PagedResponse<RecipePreviewResponse>>> resultTarget) {
        executeAsync(apiService.getRecipesByTag(tagName, page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getRecipeDetail(String recipeId, MutableLiveData<ApiResult<RecipeResponse>> resultTarget) {
        executeAsync(apiService.getRecipeDetail(recipeId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createRecipe(RecipeCreateRequestDTO request, MutableLiveData<ApiResult<RecipeResponse>> resultTarget) {
        executeAsync(apiService.createRecipe(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRecipe(String recipeId, RecipeCreateRequestDTO request, MutableLiveData<ApiResult<RecipeResponse>> resultTarget) {
        executeAsync(apiService.updateRecipe(recipeId, request), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getFavorites(MutableLiveData<ApiResult<List<RecipePreviewResponse>>> resultTarget) {
        fetchAsync(apiService::getFavorites, resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFavorite(String recipeId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.addFavorite(recipeId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFavorite(String recipeId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.removeFavorite(recipeId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPersonalNote(String recipeId, MutableLiveData<ApiResult<NoteResponse>> resultTarget) {
        executeAsync(apiService.getPersonalNote(recipeId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAllPersonalNotes(String recipeId, MutableLiveData<ApiResult<List<NoteResponse>>> resultTarget) {
        fetchAsync((page, size) -> apiService.getAllPersonalNotes(recipeId, page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveNote(String recipeId, String instructionId, String note, MutableLiveData<ApiResult<Void>> resultTarget) {
        UUID recipeUuid = UUID.fromString(recipeId);
        UUID instructionUuid = instructionId == null ? null : UUID.fromString(instructionId);
        NoteRequestDTO request = new NoteRequestDTO(recipeUuid, instructionUuid, note);
        executeAsync(apiService.saveNote(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNote(String noteId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.deleteNote(noteId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getMyRecipes(MutableLiveData<ApiResult<List<RecipePreviewResponse>>> resultTarget) {
        fetchAsync(apiService::getMyRecipes, resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPublicRecipesForUser(String userId, MutableLiveData<ApiResult<List<RecipePreviewResponse>>> resultTarget) {
        fetchAsync((page, size) -> apiService.getPublicUserRecipes(userId, page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPublicFavoritesForUser(String userId, MutableLiveData<ApiResult<List<RecipePreviewResponse>>> resultTarget) {
        fetchAsync((page, size) -> apiService.getPublicUserFavorites(userId, page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteRecipe(String recipeId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.deleteRecipe(recipeId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRecipeVisibility(String recipeId, String visibility, MutableLiveData<ApiResult<RecipeResponse>> resultTarget) {
        RecipeVisibilityUpdateRequestDTO request = new RecipeVisibilityUpdateRequestDTO(visibility);
        executeAsync(apiService.updateRecipeVisibility(recipeId, request), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void submitReview(String recipeId, double rating, String title, String comment,
                              MutableLiveData<ApiResult<Void>> resultTarget) {
        ReviewRequestDTO request = new ReviewRequestDTO(rating, title, comment);
        executeAsync(apiService.submitReview(recipeId, request), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteReview(String reviewId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.deleteReview(reviewId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reportReview(String reviewId, String reason, String comment,
                              MutableLiveData<ApiResult<Void>> resultTarget) {
        ReportReviewRequestDTO request = new ReportReviewRequestDTO(reason, comment);
        executeAsync(apiService.reportReview(reviewId, request), resultTarget);
    }
}
