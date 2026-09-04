package com.cooksync.app.data.service;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.cooksync.app.BuildConfig;
import com.cooksync.app.data.repository.AppConfigRepository;
import com.cooksync.app.data.repository.impl.AppConfigRepositoryImp;
import com.cooksync.app.domain.ApiResult;
import com.dtos.response.appconfig.AppConfigResponse;

/**
 * Process-wide singleton owning the forced-update gate: checks this build's version code against
 * the server's minimum-supported-version config, and exposes a LiveData that only ever emits
 * when this build is too old to keep using. Checked once per app start from
 * {@link com.cooksync.app.CookSyncApplication#onCreate()}, mirroring how
 * {@link com.cooksync.app.util.SessionManager} is the single source of truth for the
 * forced-logout redirect.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class VersionGateManager {

    private static VersionGateManager instance;

    private final AppConfigRepository appConfigRepository = new AppConfigRepositoryImp();

    /** Emits only when this build's version code is below the server's configured minimum. */
    private final MutableLiveData<AppConfigResponse> blockingConfig = new MutableLiveData<>();

    private VersionGateManager() {
    }

    /** @return process-wide singleton instance */
    public static synchronized VersionGateManager getInstance() {
        if (instance == null) {
            instance = new VersionGateManager();
        }
        return instance;
    }

    /**
     * LiveData that emits a non-null {@link AppConfigResponse} exactly when this build must be
     * updated before continuing, and never emits otherwise (a config check that comes back
     * "you're current," and any failed check — offline, server unreachable — both leave this
     * untouched, since a failed version check must never itself block app usage).
     *
     * @return the blocking-config event stream
     */
    public LiveData<AppConfigResponse> getBlockingConfig() {
        return blockingConfig;
    }

    /**
     * Fetches the current app-config and, if this build's version code is below the configured
     * minimum, posts it to {@link #getBlockingConfig()}.
     */
    public void checkNow() {
        MutableLiveData<ApiResult<AppConfigResponse>> result = new MutableLiveData<>();
        result.observeForever(new Observer<>() {
            @Override
            public void onChanged(ApiResult<AppConfigResponse> value) {
                if (value instanceof ApiResult.Loading) {
                    return;
                }
                result.removeObserver(this);
                if (value instanceof ApiResult.Success<AppConfigResponse> success && success.getData() != null) {
                    AppConfigResponse config = success.getData();
                    if (config.minSupportedVersionCode() > BuildConfig.VERSION_CODE) {
                        blockingConfig.postValue(config);
                    }
                }
            }
        });
        appConfigRepository.getAppConfig(result);
    }
}
