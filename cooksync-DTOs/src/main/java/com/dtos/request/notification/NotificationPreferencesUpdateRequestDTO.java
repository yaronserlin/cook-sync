package com.dtos.request.notification;

/**
 * Request payload for updating the authenticated user's notification preferences.
 *
 * @param systemAnnouncements whether the user should receive push notifications for system announcements
 * @param pushEnabled whether the user should receive push notifications at all
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public record NotificationPreferencesUpdateRequestDTO(
        boolean systemAnnouncements,
        boolean pushEnabled
) {
}
