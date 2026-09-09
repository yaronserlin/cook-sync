package com.cooksync.app.data.datasource.remote;

import com.dtos.request.device.DeviceTokenRegisterRequestDTO;
import com.dtos.response.ApiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit contract for registering and unregistering push-notification device tokens.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface DeviceApiService {

    /**
     * Registers (or refreshes) the calling device's push-notification token for the
     * authenticated user.
     *
     * @param request the device's current push token and platform
     * @return call yielding an empty acknowledgement
     */
    @POST("api/devices")
    Call<ApiResponse<Void>> registerDevice(@Body DeviceTokenRegisterRequestDTO request);

    /**
     * Removes a device's push-notification token registration, e.g. on logout.
     *
     * @param pushToken the device's FCM registration token
     * @return call yielding an empty acknowledgement
     */
    @DELETE("api/devices/{pushToken}")
    Call<ApiResponse<Void>> unregisterDevice(@Path("pushToken") String pushToken);
}
