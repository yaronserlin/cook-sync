package com.cooksync_server.services;

/**
 * Service managing per-device push-notification token registrations.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface DeviceTokenService {

    /**
     * Registers the calling device's current push token for the given user, or refreshes the
     * existing registration's last-seen timestamp if that exact token is already registered
     * (regardless of which user it was previously registered under — e.g. a shared/reset test
     * device, or a token reused across a logout/login).
     *
     * @param userEmail authenticated user email address
     * @param pushToken the device's FCM registration token
     * @param platform the device platform, e.g. "ANDROID"
     */
    void register(String userEmail, String pushToken, String platform);

    /**
     * Removes a device's push-token registration, e.g. on logout.
     *
     * @param pushToken the device's FCM registration token
     */
    void unregister(String pushToken);
}
