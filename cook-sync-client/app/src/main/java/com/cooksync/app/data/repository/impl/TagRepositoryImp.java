package com.cooksync.app.data.repository.impl;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.remote.ApiService;
import com.cooksync.app.data.datasource.remote.RetrofitClient;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.data.repository.TagRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.tags.TagResponse;

import java.util.List;

/**
 * Concrete implementation of {@link TagRepository} for remote data access, using the shared
 * call-execution machinery from {@link BaseRepository}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public class TagRepositoryImp extends BaseRepository implements TagRepository {

    private final ApiService apiService;

    /**
     * Constructs the repository against the shared authenticated Retrofit service.
     */
    public TagRepositoryImp() {
        this.apiService = RetrofitClient.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAllTags(MutableLiveData<ApiResult<List<TagResponse>>> resultTarget) {
        fetchAsync(apiService::getAllTags, resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPopularTags(int limit, MutableLiveData<ApiResult<List<TagResponse>>> resultTarget) {
        executeAsync(apiService.getPopularTags(limit), resultTarget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTag(String name, MutableLiveData<ApiResult<TagResponse>> resultTarget) {
        executeAsync(apiService.createCustomTag(new TagRequestDTO(name)), resultTarget);
    }
}
