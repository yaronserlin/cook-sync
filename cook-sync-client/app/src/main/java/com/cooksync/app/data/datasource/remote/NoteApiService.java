package com.cooksync.app.data.datasource.remote;

import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.note.NoteRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.note.NoteResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Retrofit contract for the authenticated user's private recipe/instruction notes.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface NoteApiService {

    /**
     * Fetches the private personal note attached by the user to a specific recipe.
     *
     * @param recipeId the ID of the recipe
     * @return call yielding the personal note, if any
     */
    @GET("api/notes/recipe/{recipeId}")
    Call<ApiResponse<NoteResponse>> getPersonalNote(
            @Path("recipeId") String recipeId
    );

    /**
     * Fetches every private note the user has attached to a recipe, both the general
     * recipe-wide note and any notes attached to individual instruction steps
     * (distinguished by {@link NoteResponse#instructionId()} being
     * non-null). Used by Cooking Mode to show the right note alongside each step.
     *
     * @param recipeId the ID of the recipe
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of every note (general + per-step) for the recipe
     */
    @GET("api/notes/recipe/{recipeId}/all")
    Call<ApiResponse<PagedResponse<NoteResponse>>> getAllPersonalNotes(
            @Path("recipeId") String recipeId,
            @QueryMap Map<String, String> params
    );

    /**
     * Creates or updates a personal note on a recipe (when {@code instructionId} is null) or
     * on a specific instruction step (when it's set).
     *
     * @param request the note payload
     * @return call yielding an empty acknowledgement
     */
    @POST("api/notes")
    Call<ApiResponse<Void>> saveNote(@Body NoteRequestDTO request);

    /**
     * Deletes a personal note.
     *
     * @param noteId the ID of the note to delete
     * @return call yielding an empty acknowledgement
     */
    @DELETE("api/notes/{noteId}")
    Call<ApiResponse<Void>> deleteNote(@Path("noteId") String noteId);
}
