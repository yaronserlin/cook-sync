package com.cooksync.app.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility for building a user's display name from their first and last name fields.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
public final class UserNameFormatter {

    private UserNameFormatter() {
    }

    /**
     * Joins a first and last name into a single trimmed display name, tolerating either being null.
     *
     * @param firstName the user's first name, or null
     * @param lastName the user's last name, or null
     * @return the trimmed "first last" display name, or "" if both are null
     */
    @NonNull
    public static String fullName(@Nullable String firstName, @Nullable String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }
}
