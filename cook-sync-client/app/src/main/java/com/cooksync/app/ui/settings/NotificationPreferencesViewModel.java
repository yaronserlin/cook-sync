package com.cooksync.app.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.repository.NotificationPreferencesRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.base.BaseViewModel;
import com.dtos.response.notification.NotificationPreferencesResponse;

/**
 * ViewModel backing {@link NotificationPreferencesActivity}. Unlike
 * {@link com.cooksync.app.data.datasource.local.CookingPreferencesStore}-backed screens (e.g.
 * {@link CookingPreferencesActivity}), this screen's settings are server-synced, so every toggle
 * change goes through {@link NotificationPreferencesRepository} instead of local
 * {@code SharedPreferences}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class NotificationPreferencesViewModel extends BaseViewModel {

    private final NotificationPreferencesRepository notificationPreferencesRepository;

    private final MutableLiveData<ApiResult<NotificationPreferencesResponse>> preferencesResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<Void>> updateResult = new MutableLiveData<>();

    /**
     * Constructs the ViewModel with its collaborating repository, injected by
     * {@link com.cooksync.app.ui.base.ViewModelFactory}.
     *
     * @param notificationPreferencesRepository the repository used for preference reads/writes
     */
    public NotificationPreferencesViewModel(NotificationPreferencesRepository notificationPreferencesRepository) {
        this.notificationPreferencesRepository = notificationPreferencesRepository;
    }

    /**
     * Fetches the current user's notification preferences, to populate the screen's toggles.
     */
    public void loadPreferences() {
        notificationPreferencesRepository.getPreferences(preferencesResult);
    }

    /**
     * Persists a change to the "system announcements" toggle. The other toggle's current value
     * must be supplied since the server endpoint replaces the whole preferences object.
     *
     * @param systemAnnouncements the new value for the "system announcements" toggle
     * @param pushEnabled the current value of the "push enabled" toggle, unchanged
     */
    public void setSystemAnnouncements(boolean systemAnnouncements, boolean pushEnabled) {
        notificationPreferencesRepository.updatePreferences(systemAnnouncements, pushEnabled, updateResult);
    }

    /**
     * Persists a change to the "push enabled" toggle. The other toggle's current value must be
     * supplied since the server endpoint replaces the whole preferences object.
     *
     * @param systemAnnouncements the current value of the "system announcements" toggle, unchanged
     * @param pushEnabled the new value for the "push enabled" toggle
     */
    public void setPushEnabled(boolean systemAnnouncements, boolean pushEnabled) {
        notificationPreferencesRepository.updatePreferences(systemAnnouncements, pushEnabled, updateResult);
    }

    /** @return observable result of the initial preferences fetch */
    public LiveData<ApiResult<NotificationPreferencesResponse>> getPreferencesResult() { return preferencesResult; }
    /** @return observable result of the most recent toggle save */
    public LiveData<ApiResult<Void>> getUpdateResult() { return updateResult; }
}
