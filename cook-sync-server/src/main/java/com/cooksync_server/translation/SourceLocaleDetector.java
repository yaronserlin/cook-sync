package com.cooksync_server.translation;

import java.util.List;

/**
 * Detects whether a recipe's authored text is Hebrew or English, for populating
 * {@code Recipe#sourceLocale} at creation/edit time rather than leaving it at its default.
 *
 * <p>The app only ever translates between Hebrew and English (see
 * {@code doc/vision/02-auto-translation.md}), and the two scripts never overlap, so a simple
 * Unicode-script check is a deterministic, dependency-free way to tell them apart — more reliable
 * for this specific binary problem than a general-purpose statistical language-detection library,
 * which is tuned for classifying among many languages and is notoriously unreliable on the short
 * strings a recipe is full of (a three-word title, "2 cups flour").</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
public final class SourceLocaleDetector {

    static final String HEBREW = "he";
    static final String ENGLISH = "en";

    private SourceLocaleDetector() {
    }

    /**
     * Detects the source locale from the first decisive signal, tried in order.
     *
     * @param signals candidate text in priority order (e.g. title before description before
     *                ingredient names) — a signal that is blank or contains no Hebrew or Latin
     *                letters (e.g. all-numeric) is skipped in favor of the next one
     * @return {@code "he"} if any signal contains a Hebrew letter, {@code "en"} if a signal
     *         contains a Latin letter before any Hebrew one is seen, or {@code "en"} (matching
     *         {@code Recipe#sourceLocale}'s existing default) if no signal is decisive
     */
    public static String detect(List<String> signals) {
        if (signals != null) {
            for (String signal : signals) {
                String detected = detectOne(signal);
                if (detected != null) {
                    return detected;
                }
            }
        }
        return ENGLISH;
    }

    /**
     * Convenience overload of {@link #detect(List)} for callers with plain string arguments.
     *
     * @param signals candidate text in priority order
     * @return see {@link #detect(List)}
     */
    public static String detect(String... signals) {
        return detect(signals == null ? null : List.of(signals));
    }

    private static String detectOne(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        boolean sawLatin = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isLetter(c)) {
                continue;
            }
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HEBREW) {
                return HEBREW;
            }
            sawLatin = true;
        }
        return sawLatin ? ENGLISH : null;
    }
}
