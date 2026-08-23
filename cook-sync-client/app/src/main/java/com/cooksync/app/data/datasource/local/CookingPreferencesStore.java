package com.cooksync.app.data.datasource.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.cooksync.app.CookSyncApplication;

/**
 * Persists device-level cooking preferences — the screen-awake and timer sound/vibration
 * toggles — in a plain {@link SharedPreferences} file, kept separate from {@link TokenStore}'s
 * encrypted session storage since these settings carry no sensitive data and belong to the
 * device rather than to the account. Read by
 * {@link com.cooksync.app.ui.recipe.cooking.CookingModeActivity} and written by
 * {@link com.cooksync.app.ui.settings.CookingPreferencesActivity}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public final class CookingPreferencesStore {

    /** Name of the {@link SharedPreferences} file this store reads and writes. */
    private static final String PREFS_FILE_NAME = "cooksync_cooking_prefs";

    /** Preference key for the screen-awake-during-cooking-mode toggle. */
    private static final String KEY_SCREEN_AWAKE = "screen_awake_enabled";
    /** Preference key for the timer sound/vibration toggle. */
    private static final String KEY_TIMER_SOUND = "timer_sound_enabled";

    private CookingPreferencesStore() {
    }

    /**
     * @return {@code true} if the screen should stay awake during cooking mode, defaulting to
     *         {@code true} on a fresh install
     */
    public static boolean isScreenAwakeEnabled() {
        return prefs().getBoolean(KEY_SCREEN_AWAKE, true);
    }

    /**
     * Persists whether cooking mode should keep the screen awake.
     *
     * @param enabled {@code true} to keep the screen awake while cooking mode is active
     */
    public static void setScreenAwakeEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_SCREEN_AWAKE, enabled).apply();
    }

    /**
     * @return {@code true} if a finished timer should sound and vibrate, defaulting to
     *         {@code true} on a fresh install
     */
    public static boolean isTimerSoundEnabled() {
        return prefs().getBoolean(KEY_TIMER_SOUND, true);
    }

    /**
     * Persists whether a finished step timer should play a sound and vibrate.
     *
     * @param enabled {@code true} to sound and vibrate whenever a timer finishes
     */
    public static void setTimerSoundEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_TIMER_SOUND, enabled).apply();
    }

    private static SharedPreferences prefs() {
        return CookSyncApplication.getAppContext().getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    }
}
