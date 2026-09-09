package com.cooksync.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.domain.ApiResult;
import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

/**
 * Interface contract for smart recipe-import job operations (web page or photo).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public interface RecipeImportRepository {

    /**
     * Starts a new import job.
     *
     * @param request the import request (source type + URL or already-uploaded photo URL)
     * @param resultTarget LiveData target to post the outcome
     */
    void startImport(RecipeImportStartRequestDTO request, MutableLiveData<ApiResult<RecipeImportJobResponse>> resultTarget);

    /**
     * Fetches a job's current status, for polling.
     *
     * @param jobId the job to look up
     * @param resultTarget LiveData target to post the outcome
     */
    void getJobStatus(String jobId, MutableLiveData<ApiResult<RecipeImportJobResponse>> resultTarget);
}
