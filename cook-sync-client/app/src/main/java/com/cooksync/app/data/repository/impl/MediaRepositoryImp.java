package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.data.repository.MediaRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;

/**
 * Concrete implementation of {@link MediaRepository} that delegates every call to the remote
 * REST API via Retrofit, executing network work on {@link BaseRepository}'s shared background
 * thread pool.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 04/08/2026
 */
public class MediaRepositoryImp extends BaseRepository implements MediaRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public MediaRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getUploadSignature(String folder, String publicId, MutableLiveData<ApiResult<CloudinarySignatureResponse>> resultTarget) {
        executeAsync(apiService.getMediaSignature(folder, publicId), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getBaseFolder(MutableLiveData<ApiResult<String>> resultTarget) {
        executeAsync(apiService.getCloudinaryBaseFolder(), resultTarget);
    }
}
