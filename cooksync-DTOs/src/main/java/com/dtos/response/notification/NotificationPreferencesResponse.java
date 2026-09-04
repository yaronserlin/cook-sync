package com.dtos.response.notification;

/**
 * Response payload describing the authenticated user's current notification preferences.
 *
 * @param systemAnnouncements whether the user receives push notifications for system announcements
 * @param pushEnabled whether the user receives push notifications at all
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public record NotificationPreferencesResponse(
        boolean systemAnnouncements,
        boolean pushEnabled
) {
}
