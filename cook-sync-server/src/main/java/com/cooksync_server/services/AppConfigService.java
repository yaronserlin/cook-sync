package com.cooksync_server.services;

import com.dtos.request.appconfig.AppConfigUpdateRequestDTO;
import com.dtos.response.appconfig.AppConfigResponse;

/**
 * Service managing per-platform minimum-supported-version / download-link configuration, backing
 * the client's forced-update gate.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface AppConfigService {

    /**
     * Retrieves the given platform's current configuration. If no row exists yet for the
     * platform (e.g. a future platform never configured), returns a permissive default
     * (version 1, no download URL) rather than an error, so an unconfigured platform never
     * accidentally locks every client out.
     *
     * @param platform the platform to look up, e.g. "ANDROID"
     * @return the platform's current configuration
     */
    AppConfigResponse getConfig(String platform);

    /**
     * Creates or updates a platform's minimum supported version and download link.
     *
     * @param request the new configuration
     * @param adminEmail the authenticated admin's email address
     * @return the saved configuration
     */
    AppConfigResponse updateConfig(AppConfigUpdateRequestDTO request, String adminEmail);
}
