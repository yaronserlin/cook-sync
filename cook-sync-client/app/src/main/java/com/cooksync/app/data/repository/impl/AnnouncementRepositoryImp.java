package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.AnnouncementRepository;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.response.announcement.AnnouncementResponse;

/**
 * Concrete implementation of {@link AnnouncementRepository} for remote data access, using the
 * shared call-execution machinery from {@link BaseRepository}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class AnnouncementRepositoryImp extends BaseRepository implements AnnouncementRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public AnnouncementRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    @Override
    public void getActiveAnnouncement(MutableLiveData<ApiResult<AnnouncementResponse>> resultTarget) {
        executeAsync(apiService.getActiveAnnouncement(), resultTarget);
    }

    @Override
    public void dismiss(String announcementId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.dismissAnnouncement(announcementId), resultTarget);
    }
}
