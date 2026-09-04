package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.AppVersionConfig;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.AppVersionConfigRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.appconfig.AppConfigUpdateRequestDTO;
import com.dtos.response.appconfig.AppConfigResponse;

/**
 * Unit test suite verifying per-platform minimum-supported-version / download-link retrieval
 * (including the permissive fallback for an unconfigured platform) and updates in
 * AppConfigServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@ExtendWith(MockitoExtension.class)
class AppConfigServiceTest {

    @Mock
    private AppVersionConfigRepository appVersionConfigRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppConfigServiceImp appConfigService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().id("admin-1").email("admin@cooksync.com").isAdmin(true).build();
    }

    @Test
    void getConfig_ShouldReturnExistingRow_WhenOneIsConfigured() {
        AppVersionConfig config = AppVersionConfig.builder()
                .platform("ANDROID").minSupportedVersionCode(5).downloadUrl("https://example.com/app.apk").build();
        when(appVersionConfigRepository.findById("ANDROID")).thenReturn(Optional.of(config));

        AppConfigResponse response = appConfigService.getConfig("ANDROID");

        assertEquals(5, response.minSupportedVersionCode());
        assertEquals("https://example.com/app.apk", response.downloadUrl());
    }

    @Test
    void getConfig_ShouldReturnPermissiveDefault_WhenPlatformNeverConfigured() {
        // An unconfigured platform (e.g. one never set up yet) must never accidentally lock
        // every client out — default to version 1 (blocks nothing) rather than erroring.
        when(appVersionConfigRepository.findById("IOS")).thenReturn(Optional.empty());

        AppConfigResponse response = appConfigService.getConfig("IOS");

        assertEquals("IOS", response.platform());
        assertEquals(1, response.minSupportedVersionCode());
        assertNull(response.downloadUrl());
    }

    @Test
    void updateConfig_ShouldThrowResourceNotFoundException_WhenAdminMissing() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> appConfigService.updateConfig(
                new AppConfigUpdateRequestDTO("ANDROID", 2, "https://example.com/app.apk"), "missing@cooksync.com"));

        verify(appVersionConfigRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void updateConfig_ShouldUpdateExistingRow() {
        AppVersionConfig existing = AppVersionConfig.builder().platform("ANDROID").minSupportedVersionCode(1).build();
        when(userRepository.findByEmail("admin@cooksync.com")).thenReturn(Optional.of(admin));
        when(appVersionConfigRepository.findById("ANDROID")).thenReturn(Optional.of(existing));
        when(appVersionConfigRepository.save(any(AppVersionConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppConfigResponse response = appConfigService.updateConfig(
                new AppConfigUpdateRequestDTO("ANDROID", 3, "https://example.com/app.apk"), "admin@cooksync.com");

        ArgumentCaptor<AppVersionConfig> captor = ArgumentCaptor.forClass(AppVersionConfig.class);
        verify(appVersionConfigRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getMinSupportedVersionCode());
        assertEquals("https://example.com/app.apk", captor.getValue().getDownloadUrl());
        assertEquals(admin, captor.getValue().getUpdatedBy());
        assertEquals(3, response.minSupportedVersionCode());
    }

    @Test
    void updateConfig_ShouldCreateNewRow_WhenPlatformNeverConfigured() {
        when(userRepository.findByEmail("admin@cooksync.com")).thenReturn(Optional.of(admin));
        when(appVersionConfigRepository.findById("IOS")).thenReturn(Optional.empty());
        when(appVersionConfigRepository.save(any(AppVersionConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appConfigService.updateConfig(new AppConfigUpdateRequestDTO("IOS", 1, null), "admin@cooksync.com");

        ArgumentCaptor<AppVersionConfig> captor = ArgumentCaptor.forClass(AppVersionConfig.class);
        verify(appVersionConfigRepository).save(captor.capture());
        assertEquals("IOS", captor.getValue().getPlatform());
    }
}
