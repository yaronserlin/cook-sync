package com.cooksync_server.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.entities.AppVersionConfig;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.AppVersionConfigRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.appconfig.AppConfigUpdateRequestDTO;
import com.dtos.response.appconfig.AppConfigResponse;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing per-platform minimum-supported-version / download-link configuration.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Service
@RequiredArgsConstructor
public class AppConfigServiceImp implements AppConfigService {

    /** Permissive fallback used when a platform has no configured row at all. */
    private static final int DEFAULT_MIN_VERSION_CODE = 1;

    private final AppVersionConfigRepository appVersionConfigRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AppConfigResponse getConfig(String platform) {
        return appVersionConfigRepository.findById(platform)
                .map(this::toResponse)
                .orElseGet(() -> new AppConfigResponse(platform, DEFAULT_MIN_VERSION_CODE, null));
    }

    @Override
    @Transactional
    public AppConfigResponse updateConfig(AppConfigUpdateRequestDTO request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, adminEmail));

        AppVersionConfig config = appVersionConfigRepository.findById(request.platform())
                .orElseGet(() -> AppVersionConfig.builder().platform(request.platform()).build());
        config.setMinSupportedVersionCode(request.minSupportedVersionCode());
        config.setDownloadUrl(request.downloadUrl());
        config.setUpdatedBy(admin);

        return toResponse(appVersionConfigRepository.save(config));
    }

    private AppConfigResponse toResponse(AppVersionConfig config) {
        return new AppConfigResponse(config.getPlatform(), config.getMinSupportedVersionCode(), config.getDownloadUrl());
    }
}
