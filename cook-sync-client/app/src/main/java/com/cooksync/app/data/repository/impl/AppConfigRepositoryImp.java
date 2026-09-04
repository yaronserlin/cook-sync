package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.AppConfigRepository;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.response.appconfig.AppConfigResponse;

/**
 * Concrete implementation of {@link AppConfigRepository} for remote data access, using the
 * shared call-execution machinery from {@link BaseRepository}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class AppConfigRepositoryImp extends BaseRepository implements AppConfigRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared Retrofit service. Reachable even without a
     * logged-in session — the underlying endpoint is unauthenticated — since
     * {@link RetrofitClient}'s auth interceptor simply omits the header when no token is stored.
     */
    public AppConfigRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    @Override
    public void getAppConfig(MutableLiveData<ApiResult<AppConfigResponse>> resultTarget) {
        executeAsync(apiService.getAppConfig("ANDROID"), resultTarget);
    }
}
