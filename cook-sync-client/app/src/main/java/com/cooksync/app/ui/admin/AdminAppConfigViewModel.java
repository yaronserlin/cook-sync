package com.cooksync.app.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.repository.AdminRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.base.BaseViewModel;
import com.dtos.response.appconfig.AppConfigResponse;

/**
 * Manages the Admin Console's "App version" settings card: the Android platform's minimum
 * supported version code and download link, which back the client's forced-update gate
 * ({@code VersionGateManager}). A separate ViewModel from {@link AdminAnnouncementsViewModel}
 * even though both render into the Announcements tab's layout — the two settle different,
 * independently-loading concerns and there's no reason to couple them.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class AdminAppConfigViewModel extends BaseViewModel {

    private final AdminRepository adminRepository;

    private final MutableLiveData<ApiResult<AppConfigResponse>> configResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<AppConfigResponse>> saveResult = new MutableLiveData<>();

    /**
     * Constructs the ViewModel with the given repository, injected by
     * {@link com.cooksync.app.ui.base.ViewModelFactory}.
     *
     * @param adminRepository the repository used for the app-config calls
     */
    public AdminAppConfigViewModel(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    /** @return observable result of the current app-config fetch */
    public LiveData<ApiResult<AppConfigResponse>> getConfigResult() { return configResult; }
    /** @return observable result of the most recent save */
    public LiveData<ApiResult<AppConfigResponse>> getSaveResult() { return saveResult; }

    /**
     * Loads the Android platform's current app-config, to populate the settings card's fields.
     */
    public void loadConfig() {
        adminRepository.getAppConfig(configResult);
    }

    /**
     * Saves a new minimum supported version code and download link.
     *
     * @param minSupportedVersionCode the lowest client version code still allowed to use the app
     * @param downloadUrl where users should go to download the current build
     */
    public void saveConfig(int minSupportedVersionCode, String downloadUrl) {
        adminRepository.updateAppConfig(minSupportedVersionCode, downloadUrl, saveResult);
    }
}
