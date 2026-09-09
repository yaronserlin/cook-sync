package com.cooksync.app.data.datasource.remote;

import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.tags.TagResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Retrofit contract for recipe-tag browsing and creation endpoints.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface TagApiService {

    /**
     * Fetches a page of available tags for the horizontal filter bar.
     *
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of tags
     */
    @GET("api/tags")
    Call<ApiResponse<PagedResponse<TagResponse>>> getAllTags(@QueryMap Map<String, String> params);

    /**
     * Fetches the most-used tags across all recipes, ranked by descending recipe count.
     *
     * @param limit maximum number of popular tags to return
     * @return call yielding the popular tags ordered by descending usage
     */
    @GET("api/tags/popular")
    Call<ApiResponse<List<TagResponse>>> getPopularTags(
            @Query("limit") int limit
    );

    /**
     * Creates a new custom tag, or returns the existing one if a tag with the same name
     * (case-insensitive) already exists.
     *
     * @param request the tag name payload
     * @return call yielding the created (or matched) tag
     */
    @POST("api/tags/custom")
    Call<ApiResponse<TagResponse>> createCustomTag(
            @Body TagRequestDTO request
    );
}
