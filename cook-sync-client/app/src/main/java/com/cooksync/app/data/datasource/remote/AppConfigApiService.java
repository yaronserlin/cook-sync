package com.cooksync.app.data.datasource.remote;

import com.dtos.request.appconfig.AppConfigUpdateRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.appconfig.AppConfigResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * Retrofit contract for the platform-specific minimum-supported-version app configuration.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface AppConfigApiService {

    /**
     * Fetches the minimum supported client version and download link for a platform.
     * Unauthenticated — must be reachable before login.
     *
     * @param platform the platform to look up, e.g. "ANDROID"
     * @return call yielding the platform's current app-config
     */
    @GET("api/app-config")
    Call<ApiResponse<AppConfigResponse>> getAppConfig(@Query("platform") String platform);

    /**
     * Updates a platform's minimum supported client version and download link (admin-only).
     *
     * @param request the new configuration
     * @return call yielding the saved app-config
     */
    @PUT("api/admin/app-config")
    Call<ApiResponse<AppConfigResponse>> updateAppConfig(@Body AppConfigUpdateRequestDTO request);
}
