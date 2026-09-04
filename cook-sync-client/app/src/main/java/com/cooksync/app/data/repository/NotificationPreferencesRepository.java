package com.cooksync.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.domain.ApiResult;
import com.dtos.response.notification.NotificationPreferencesResponse;

/**
 * Interface contract for the authenticated user's notification preferences. Unlike
 * {@link com.cooksync.app.data.datasource.local.CookingPreferencesStore}, these preferences are
 * server-synced (stored per-account, not per-device), so this is a repository backed by the
 * network rather than local {@code SharedPreferences}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface NotificationPreferencesRepository {

    /**
     * Fetches the authenticated user's current notification preferences.
     *
     * @param resultTarget LiveData target to post the outcome
     */
    void getPreferences(MutableLiveData<ApiResult<NotificationPreferencesResponse>> resultTarget);

    /**
     * Updates the authenticated user's notification preferences.
     *
     * @param systemAnnouncements whether the user should receive push notifications for system announcements
     * @param pushEnabled whether the user should receive push notifications at all
     * @param resultTarget LiveData target to post the outcome
     */
    void updatePreferences(boolean systemAnnouncements, boolean pushEnabled, MutableLiveData<ApiResult<Void>> resultTarget);
}
