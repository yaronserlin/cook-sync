package com.dtos.request.appconfig;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for an admin updating a platform's minimum supported client version and
 * download link.
 *
 * @param platform the platform this config applies to, e.g. "ANDROID"
 * @param minSupportedVersionCode the lowest client version code still allowed to use the app
 * @param downloadUrl where users should go to download the current build; may be blank if not
 * yet available
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public record AppConfigUpdateRequestDTO(
        @NotBlank(message = "Platform is required")
        String platform,
        @Min(value = 1, message = "Minimum supported version code must be at least 1")
        int minSupportedVersionCode,
        String downloadUrl
) {
}
