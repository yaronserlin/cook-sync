package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.data.repository.NotificationPreferencesRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.request.notification.NotificationPreferencesUpdateRequestDTO;
import com.dtos.response.notification.NotificationPreferencesResponse;

/**
 * Concrete implementation of {@link NotificationPreferencesRepository} for remote data access,
 * using the shared call-execution machinery from {@link BaseRepository}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class NotificationPreferencesRepositoryImp extends BaseRepository implements NotificationPreferencesRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public NotificationPreferencesRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    @Override
    public void getPreferences(MutableLiveData<ApiResult<NotificationPreferencesResponse>> resultTarget) {
        executeAsync(apiService.getNotificationPreferences(), resultTarget);
    }

    @Override
    public void updatePreferences(boolean systemAnnouncements, boolean pushEnabled, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.updateNotificationPreferences(
                new NotificationPreferencesUpdateRequestDTO(systemAnnouncements, pushEnabled)), resultTarget);
    }
}
