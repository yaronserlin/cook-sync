package com.cooksync_server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.constants.ApiRoutes;
import com.cooksync_server.services.AppConfigService;
import com.dtos.response.ApiResponse;
import com.dtos.response.appconfig.AppConfigResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller exposing the public, unauthenticated app-config lookup that backs the
 * client's forced-update gate. Deliberately outside {@link AdminController} (which is
 * admin-only) — this endpoint must be reachable by every client, logged in or not. The
 * corresponding admin-only update endpoint lives on {@link AdminController} instead.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@RestController
@RequestMapping(ApiRoutes.APP_CONFIG)
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigService appConfigService;

    /**
     * Retrieves the minimum supported client version and download link for a platform.
     *
     * @param platform the platform to look up, e.g. "ANDROID" (defaults to "ANDROID" since
     * that's the only client that exists today)
     * @return response entity containing the platform's current configuration
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AppConfigResponse>> getConfig(
            @RequestParam(defaultValue = "ANDROID") String platform) {
        return ResponseEntity.ok(ApiResponse.success(appConfigService.getConfig(platform), "App config retrieved successfully"));
    }
}
