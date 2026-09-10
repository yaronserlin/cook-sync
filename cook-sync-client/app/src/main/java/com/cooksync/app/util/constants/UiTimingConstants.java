package com.cooksync.app.util.constants;

/**
 * Shared UI timing constants (debounce windows, resend cooldowns, toast/undo durations, submit
 * rate limits) previously declared separately at each call site, several as identical
 * independently-maintained copies of the same value.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 27/08/2026
 */
public final class UiTimingConstants {

    /** How long to wait after the last keystroke before running a live search. */
    public static final long SEARCH_DEBOUNCE_MS = 350L;

    /** Seconds a resend-code button stays disabled after a code is issued or resent. */
    public static final int RESEND_COOLDOWN_SECONDS = 30;

    /**
     * How long a toast stays on screen before auto-dismissing, in milliseconds, and — shared
     * deliberately with {@code BaseRepository.UNDO_WINDOW_MS} — how long an "act now, send
     * later" optimistic action is deferred before it actually reaches the server, so the undo
     * window lasts exactly as long as the toast offering it is visible.
     */
    public static final long UNDO_TOAST_DURATION_MS = 3200;

    /** Minimum milliseconds between successive login or registration submit attempts. */
    public static final long SUBMIT_COOLDOWN_MS = 2000L;

    private UiTimingConstants() {}
}
