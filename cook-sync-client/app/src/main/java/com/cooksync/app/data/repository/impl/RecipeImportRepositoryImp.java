package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.data.repository.RecipeImportRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

/**
 * Concrete implementation of {@link RecipeImportRepository} for remote data access, using the
 * shared call-execution machinery from {@link BaseRepository}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class RecipeImportRepositoryImp extends BaseRepository implements RecipeImportRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public RecipeImportRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startImport(RecipeImportStartRequestDTO request, MutableLiveData<ApiResult<RecipeImportJobResponse>> resultTarget) {
        executeAsync(apiService.startRecipeImport(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getJobStatus(String jobId, MutableLiveData<ApiResult<RecipeImportJobResponse>> resultTarget) {
        executeAsync(apiService.getRecipeImportStatus(jobId), resultTarget);
    }
}
