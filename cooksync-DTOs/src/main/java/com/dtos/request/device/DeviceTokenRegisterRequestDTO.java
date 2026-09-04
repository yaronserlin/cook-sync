package com.dtos.request.device;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for registering (or refreshing) the calling device's push-notification token.
 * Submitted after login and on every app launch by the Android client so the server always has
 * an up-to-date token to deliver push notifications and system-announcement broadcasts to.
 *
 * @param pushToken the device's current FCM registration token
 * @param platform the device platform the token belongs to, e.g. "ANDROID"
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public record DeviceTokenRegisterRequestDTO(
        @NotBlank(message = "Push token is required")
        String pushToken,
        @NotBlank(message = "Platform is required")
        String platform
) {
}
