package com.cooksync_server.services;

/**
 * Service sending push notifications to registered devices, backed by the Firebase Admin SDK.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface PushNotificationService {

    /**
     * Sends a push notification to a single device token.
     *
     * @param pushToken the target device's FCM registration token
     * @param title the notification's title
     * @param body the notification's body text
     * @return {@code true} if the push was actually sent, {@code false} if it was skipped
     * (Firebase not configured) or failed
     */
    boolean sendToDevice(String pushToken, String title, String body);

    /**
     * Broadcasts a push notification to every device token belonging to users who currently have
     * push notifications enabled. Runs on a dedicated bounded background thread pool and returns
     * immediately rather than blocking the caller.
     *
     * @param title the notification's title
     * @param body the notification's body text
     */
    void broadcast(String title, String body);
}
