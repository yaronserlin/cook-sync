package com.dtos.response.announcement;

/**
 * Response payload describing one system announcement.
 *
 * @param id the announcement's unique ID
 * @param title the announcement's short headline
 * @param body the announcement's full message body
 * @param severity "INFO" or "ACTION_REQUIRED"
 * @param active whether the announcement is still active (relevant on the admin listing; the
 * user-facing "active" endpoint only ever returns an active, not-yet-dismissed announcement)
 * @param createdAt ISO-formatted creation timestamp string, when the announcement was created
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public record AnnouncementResponse(
        String id,
        String title,
        String body,
        String severity,
        boolean active,
        String createdAt
) {
}
