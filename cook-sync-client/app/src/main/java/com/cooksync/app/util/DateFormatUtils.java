package com.cooksync.app.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Parsing helper for the ISO-8601 timestamp strings ({@code createdAt}/{@code updatedAt}) the
 * server sends on every DTO, previously reimplemented near-identically at each call site that
 * needed to turn one into a {@link LocalDate} for display formatting.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
public final class DateFormatUtils {

    private DateFormatUtils() {
    }

    /**
     * Parses the date portion of an ISO-8601 timestamp string.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param isoTimestamp the timestamp string, e.g. a DTO's {@code createdAt} value
     * @return the parsed date, or {@code null} if {@code isoTimestamp} is {@code null}, blank,
     *         or not a valid ISO date
     */
    @Nullable
    public static LocalDate parseIsoDate(@Nullable String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoTimestamp.substring(0, 10));
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Formats an ISO-8601 timestamp into a short, day-granularity relative label ("Today",
     * "3 days ago", "2 months ago", ...). Unlike {@link RelativeTimeFormatter}, which formats an
     * epoch-millis instant down to minute/hour precision, this compares calendar dates only, since
     * the server's {@code createdAt}/{@code updatedAt} strings carry no timezone offset.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param isoTimestamp the timestamp string, e.g. a DTO's {@code createdAt} value
     * @return a human-readable relative-time label, or "" if {@code isoTimestamp} is unparseable
     */
    @NonNull
    public static String formatRelativeDay(@Nullable String isoTimestamp) {
        LocalDate date = parseIsoDate(isoTimestamp);
        if (date == null) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days <= 0) {
            return "Today";
        } else if (days == 1) {
            return "1 day ago";
        } else if (days < 30) {
            return days + " days ago";
        } else if (days < 365) {
            long months = days / 30;
            return months + (months == 1 ? " month ago" : " months ago");
        } else {
            long years = days / 365;
            return years + (years == 1 ? " year ago" : " years ago");
        }
    }
}
