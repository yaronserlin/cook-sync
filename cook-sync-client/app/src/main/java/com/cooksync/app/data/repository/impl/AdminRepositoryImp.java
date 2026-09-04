package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.AdminRepository;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.request.announcement.AnnouncementCreateRequestDTO;
import com.dtos.request.appconfig.AppConfigUpdateRequestDTO;
import com.dtos.request.tags.TagMergeRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.AdminStatsResponse;
import com.dtos.response.admin.DuplicateTagGroupResponse;
import com.dtos.response.admin.ReportedReviewResponse;
import com.dtos.response.announcement.AnnouncementResponse;
import com.dtos.response.appconfig.AppConfigResponse;
import com.dtos.response.user.UserResponse;

/**
 * Concrete implementation of {@link AdminRepository} that delegates calls to the remote
 * {@link ApiService} and manages execution on a background thread pool (inherited from
 * {@link BaseRepository}).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public class AdminRepositoryImp extends BaseRepository implements AdminRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public AdminRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getStats(MutableLiveData<ApiResult<AdminStatsResponse>> resultTarget) {
        executeAsync(apiService.getAdminStats(), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getUsers(int page, int size, String q, Boolean enabled, String sortBy, String direction,
                          MutableLiveData<ApiResult<PagedResponse<UserResponse>>> resultTarget) {
        executeAsync(apiService.getAdminUsers(page, size, q, enabled, sortBy, direction), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getReportedReviews(int page, int size,
                                    MutableLiveData<ApiResult<PagedResponse<ReportedReviewResponse>>> resultTarget) {
        executeAsync(apiService.getReportedReviews(page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dismissReport(String reviewId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.dismissReport(reviewId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableUser(String userId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.enableUser(userId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void suspendUser(String userId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.suspendUser(userId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteUser(String userId, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.deleteUser(userId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getDuplicateTagGroups(int page, int size,
                                       MutableLiveData<ApiResult<PagedResponse<DuplicateTagGroupResponse>>> resultTarget) {
        executeAsync(apiService.getDuplicateTagGroups(page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mergeTags(String sourceTagId, String targetTagId, MutableLiveData<ApiResult<Void>> resultTarget) {
        TagMergeRequestDTO request = new TagMergeRequestDTO(sourceTagId, targetTagId);
        executeAsync(apiService.mergeTags(request), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createAnnouncement(String title, String body, String severity,
                                    MutableLiveData<ApiResult<AnnouncementResponse>> resultTarget) {
        executeAsync(apiService.createAnnouncement(new AnnouncementCreateRequestDTO(title, body, severity)), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAnnouncements(int page, int size,
                                  MutableLiveData<ApiResult<PagedResponse<AnnouncementResponse>>> resultTarget) {
        executeAsync(apiService.getAnnouncements(page, size), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deactivateAnnouncement(String id, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.deactivateAnnouncement(id), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAppConfig(MutableLiveData<ApiResult<AppConfigResponse>> resultTarget) {
        executeAsync(apiService.getAppConfig("ANDROID"), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateAppConfig(int minSupportedVersionCode, String downloadUrl,
                                 MutableLiveData<ApiResult<AppConfigResponse>> resultTarget) {
        executeAsync(apiService.updateAppConfig(
                new AppConfigUpdateRequestDTO("ANDROID", minSupportedVersionCode, downloadUrl)), resultTarget);
    }
}
