package com.dtos.request.announcement;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for an admin creating and broadcasting a new system announcement.
 *
 * @param title the announcement's short headline, shown as the in-app dialog's title
 * @param body the announcement's full message body
 * @param severity "INFO" for a routine notice, or "ACTION_REQUIRED" for one the user cannot
 * permanently silence via notification preferences (e.g. a mandatory app update)
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public record AnnouncementCreateRequestDTO(
        @NotBlank(message = "Title is required")
        String title,
        @NotBlank(message = "Body is required")
        String body,
        @NotBlank(message = "Severity is required (INFO, ACTION_REQUIRED)")
        String severity
) {
}
