package com.cooksync.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.domain.ApiResult;
import com.dtos.response.appconfig.AppConfigResponse;

/**
 * Interface contract for the public, unauthenticated app-config lookup that backs the client's
 * forced-update gate.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface AppConfigRepository {

    /**
     * Fetches the minimum supported client version and download link for this platform
     * ("ANDROID").
     *
     * @param resultTarget LiveData target to post the outcome
     */
    void getAppConfig(MutableLiveData<ApiResult<AppConfigResponse>> resultTarget);
}
