package com.cooksync_server.mappers;

import java.time.LocalDateTime;

/**
 * Shared helper methods utilized by mapper classes for null-safe formatting.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
final class MapperUtils {

    private MapperUtils() {
    }

    /**
     * Formats a LocalDateTime instance to ISO string format or null.
     *
     * @param dateTime target LocalDateTime instance
     * @return ISO-formatted string representation or null if input is null
     */
    static String toIsoStringOrNull(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
