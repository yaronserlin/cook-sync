package com.cooksync.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.domain.ApiResult;

/**
 * Interface contract for push-notification device-token registration.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface DeviceTokenRepository {

    /**
     * Registers (or refreshes) this device's current push token for the authenticated user.
     * Called after login and on every app launch.
     *
     * @param pushToken the device's current FCM registration token
     * @param platform the device platform, e.g. "ANDROID"
     * @param resultTarget LiveData target to post the outcome
     */
    void registerDevice(String pushToken, String platform, MutableLiveData<ApiResult<Void>> resultTarget);

    /**
     * Removes this device's push-token registration, e.g. on logout.
     *
     * @param pushToken the device's FCM registration token
     * @param resultTarget LiveData target to post the outcome
     */
    void unregisterDevice(String pushToken, MutableLiveData<ApiResult<Void>> resultTarget);
}
