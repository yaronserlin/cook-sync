package com.cooksync.app.data.datasource.remote;

import com.dtos.request.notification.NotificationPreferencesUpdateRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.notification.NotificationPreferencesResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;

/**
 * Retrofit contract for the authenticated user's notification preferences.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface NotificationPreferencesApiService {

    /**
     * Fetches the authenticated user's current notification preferences.
     *
     * @return call yielding the user's notification preferences
     */
    @GET("api/notification-preferences")
    Call<ApiResponse<NotificationPreferencesResponse>> getNotificationPreferences();

    /**
     * Updates the authenticated user's notification preferences.
     *
     * @param request the new preference values
     * @return call yielding an empty acknowledgement
     */
    @PUT("api/notification-preferences")
    Call<ApiResponse<Void>> updateNotificationPreferences(@Body NotificationPreferencesUpdateRequestDTO request);
}
