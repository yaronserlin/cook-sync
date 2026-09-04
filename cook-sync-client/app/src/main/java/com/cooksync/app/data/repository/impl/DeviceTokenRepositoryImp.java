package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.data.repository.DeviceTokenRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.request.device.DeviceTokenRegisterRequestDTO;

/**
 * Concrete implementation of {@link DeviceTokenRepository} for remote data access, using the
 * shared call-execution machinery from {@link BaseRepository}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class DeviceTokenRepositoryImp extends BaseRepository implements DeviceTokenRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public DeviceTokenRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    @Override
    public void registerDevice(String pushToken, String platform, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.registerDevice(new DeviceTokenRegisterRequestDTO(pushToken, platform)), resultTarget);
    }

    @Override
    public void unregisterDevice(String pushToken, MutableLiveData<ApiResult<Void>> resultTarget) {
        executeAsync(apiService.unregisterDevice(pushToken), resultTarget);
    }
}
