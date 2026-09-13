package com.cooksync.app.util;

import android.view.View;

/**
 * Detects whether a piece of recipe text is written in Hebrew or Latin script, so a recipe screen
 * can set its layout/text direction from the language the recipe's content actually came back in
 * rather than from the device's own locale — a poor proxy once server-side translation is
 * involved, since a recipe whose translation failed still renders in its original language
 * regardless of the device's locale. The server resolves a whole recipe's translatable fields as
 * one all-or-nothing attempt (see {@code RecipeTranslationCoordinator} on the server), so a
 * recipe's content is always internally one language — detecting from a single representative
 * field (typically the title) is enough for the whole screen.
 *
 * <p>Mirrors cook-sync-server's {@code SourceLocaleDetector} Hebrew-vs-English detection
 * algorithm as a standalone copy, since the client can't share server code. Uses
 * {@link Character.UnicodeBlock} (available since API 1) rather than
 * {@code Character.UnicodeScript} (API 24+, matching but not comfortably below this app's own
 * minSdk) simply to avoid coupling this detector's availability to the app's minSdk at all.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
public final class HebrewScriptDetector {

    private HebrewScriptDetector() {
    }

    /**
     * Detects direction from the first decisive signal, tried in order.
     *
     * @param signals candidate text in priority order (e.g. title before description) — a signal
     *                that is blank or contains no Hebrew or Latin letters (e.g. all-numeric) is
     *                skipped in favor of the next one
     * @return {@code true} if any signal contains a Hebrew letter, {@code false} if a signal
     *         contains a Latin letter before any Hebrew one is seen, or {@code false} (LTR) if no
     *         signal is decisive
     */
    public static boolean isRtl(String... signals) {
        if (signals != null) {
            for (String signal : signals) {
                Boolean decided = isRtlOne(signal);
                if (decided != null) {
                    return decided;
                }
            }
        }
        return false;
    }

    /**
     * Convenience wrapper over {@link #isRtl} returning an Android layout-direction constant.
     *
     * @param signals candidate text in priority order
     * @return {@link View#LAYOUT_DIRECTION_RTL} or {@link View#LAYOUT_DIRECTION_LTR}
     */
    public static int layoutDirection(String... signals) {
        return isRtl(signals) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
    }

    private static Boolean isRtlOne(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        boolean sawLatin = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isLetter(c)) {
                continue;
            }
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HEBREW) {
                return Boolean.TRUE;
            }
            sawLatin = true;
        }
        return sawLatin ? Boolean.FALSE : null;
    }
}
