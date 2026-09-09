package com.cooksync.app.data.datasource.remote;

import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.unit.UnitRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.unit.UnitResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Retrofit contract for measurement-unit browsing and admin management endpoints.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface UnitApiService {

    /**
     * Fetches a page of measurement units available for recipe ingredients.
     *
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a paged collection of units
     */
    @GET("api/units")
    Call<ApiResponse<PagedResponse<UnitResponse>>> getUnits(@QueryMap Map<String, String> params);

    /**
     * Creates a new measurement unit. Admin-only.
     *
     * @param request unit creation request DTO
     * @return call yielding the created unit
     */
    @POST("api/units")
    Call<ApiResponse<UnitResponse>> createUnit(@Body UnitRequestDTO request);

    /**
     * Deletes a measurement unit by ID. Admin-only.
     *
     * @param id target unit unique identifier
     * @return call acknowledging the deletion
     */
    @DELETE("api/units/{id}")
    Call<ApiResponse<Void>> deleteUnit(@Path("id") String id);
}
