package com.dtos.response.appconfig;

/**
 * Response payload describing the minimum supported client version for a platform, and where to
 * download an update. Served unauthenticated, since it must be checkable before a user has
 * logged in (or even on a version too old to reach the login screen's own logic correctly).
 *
 * @param platform the platform this config applies to, e.g. "ANDROID"
 * @param minSupportedVersionCode the lowest client version code still allowed to use the app; a
 * caller running below this must update before continuing
 * @param downloadUrl where to send the user to download the current build, or {@code null} if
 * not yet configured
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public record AppConfigResponse(
        String platform,
        int minSupportedVersionCode,
        String downloadUrl
) {
}
