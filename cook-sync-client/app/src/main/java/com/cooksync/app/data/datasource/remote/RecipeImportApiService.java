package com.cooksync.app.data.datasource.remote;

import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit contract for starting and polling smart recipe-import jobs (web page or photo).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface RecipeImportApiService {

    /**
     * Starts a smart recipe-import job (web page or photo) and returns immediately with its
     * initial (PENDING) status — extraction runs server-side in the background.
     *
     * @param request the import request
     * @return call yielding the newly created job's status
     */
    @POST("api/recipe-imports")
    Call<ApiResponse<RecipeImportJobResponse>> startRecipeImport(@Body RecipeImportStartRequestDTO request);

    /**
     * Fetches a recipe-import job's current status, for polling.
     *
     * @param jobId the job to look up
     * @return call yielding the job's current status
     */
    @GET("api/recipe-imports/{jobId}")
    Call<ApiResponse<RecipeImportJobResponse>> getRecipeImportStatus(@Path("jobId") String jobId);
}
