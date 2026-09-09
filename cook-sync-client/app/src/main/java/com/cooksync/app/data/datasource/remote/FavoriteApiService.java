package com.cooksync.app.data.datasource.remote;

import com.dtos.request.common.PageRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Retrofit contract for the authenticated user's favorites list.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface FavoriteApiService {

    /**
     * Fetches a page of recipes favorited by the currently authenticated user.
     *
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of the user's favorites
     */
    @GET("api/favorites")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getFavorites(@QueryMap Map<String, String> params);

    /**
     * Adds a recipe to the user's favorites list.
     *
     * @param recipeId the ID of the recipe to favorite
     * @return call yielding an empty response
     */
    @POST("api/favorites/{recipeId}")
    Call<ApiResponse<Void>> addFavorite(@Path("recipeId") String recipeId);

    /**
     * Removes a recipe from the user's favorites list.
     *
     * @param recipeId the ID of the recipe to unfavorite
     * @return call yielding an empty response
     */
    @DELETE("api/favorites/{recipeId}")
    Call<ApiResponse<Void>> removeFavorite(@Path("recipeId") String recipeId);
}
