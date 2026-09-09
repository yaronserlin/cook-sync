package com.cooksync.app.data.datasource.remote;

import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeFeedRequestDTO;
import com.dtos.request.recipe.RecipeSearchRequestDTO;
import com.dtos.request.recipe.RecipeTagFilterRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Retrofit contract for recipe browsing, authoring, and management endpoints.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface RecipeApiService {

    /**
     * Fetches a paginated list of public recipe previews for the home feed.
     *
     * @param params {@link RecipeFeedRequestDTO#toQueryMap()} for the feed's pagination, sort,
     *               and facet-filter parameters
     * @return call yielding a paged collection of recipe previews
     */
    @GET("api/recipes/paged")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getPublicFeed(@QueryMap Map<String, String> params);

    /**
     * Searches for public recipes matching a text query, author, or ingredient.
     *
     * @param params {@link RecipeSearchRequestDTO#toQueryMap()} for the search's keyword/facet,
     *               pagination, and sort parameters
     * @return call yielding a paged collection of matching recipe previews
     */
    @GET("api/recipes/search")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> searchRecipes(@QueryMap Map<String, String> params);

    /**
     * Fetches public recipes associated with a specific tag.
     *
     * @param tagName the name of the tag to filter by
     * @param params {@link RecipeTagFilterRequestDTO#toQueryMap()} for the browse's pagination,
     *               sort, and facet-filter parameters
     * @return call yielding a paged collection of recipe previews
     */
    @GET("api/recipes/tag/{tagName}")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getRecipesByTag(
            @Path("tagName") String tagName,
            @QueryMap Map<String, String> params
    );

    /**
     * Fetches the complete details for a specific recipe.
     *
     * @param id the unique identifier of the recipe
     * @return call yielding the full recipe detail
     */
    @GET("api/recipes/{id}")
    Call<ApiResponse<RecipeResponse>> getRecipeDetail(
            @Path("id") String id
    );

    /**
     * Creates a new recipe authored by the currently authenticated user.
     *
     * @param request the complete recipe payload (metadata, ingredients, instructions, tags)
     * @return call yielding the newly created recipe
     */
    @POST("api/recipes")
    Call<ApiResponse<RecipeResponse>> createRecipe(
            @Body RecipeCreateRequestDTO request
    );

    /**
     * Updates an existing recipe owned by the currently authenticated user.
     *
     * @param id the unique identifier of the recipe to update
     * @param request the complete updated recipe payload
     * @return call yielding the updated recipe response
     */
    @PUT("api/recipes/{id}")
    Call<ApiResponse<RecipeResponse>> updateRecipe(
            @Path("id") String id,
            @Body RecipeCreateRequestDTO request
    );

    /**
     * Fetches every recipe (published or private) authored by the currently authenticated
     * user, for the "My Recipes" screen.
     *
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of the user's own recipes
     */
    @GET("api/recipes/mine")
    Call<ApiResponse<PagedResponse<RecipePreviewResponse>>> getMyRecipes(@QueryMap Map<String, String> params);

    /**
     * Deletes one of the authenticated user's own recipes.
     *
     * @param id the ID of the recipe to delete
     * @return call yielding an empty acknowledgement
     */
    @DELETE("api/recipes/{id}")
    Call<ApiResponse<Void>> deleteRecipe(@Path("id") String id);

    /**
     * Changes only a recipe's visibility (Public/Private) without resubmitting the rest of
     * its fields.
     *
     * @param id the ID of the recipe to update
     * @param request the new visibility
     * @return call yielding the updated recipe
     */
    @PATCH("api/recipes/{id}/visibility")
    Call<ApiResponse<RecipeResponse>> updateRecipeVisibility(
            @Path("id") String id,
            @Body RecipeVisibilityUpdateRequestDTO request
    );
}
