package com.cooksync_server.translation;

import java.util.Optional;

/**
 * Abstraction over an external machine-translation backend, kept separate from
 * {@link com.cooksync_server.services.TranslationService} so the caching/fallback policy never
 * has to change when the underlying provider does (e.g. swapping in Google Cloud Translation or
 * Azure Translator later just means adding a new implementation and wiring it in as the
 * {@code @Primary} bean).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/09/2026
 */
public interface TranslationProvider {

    /**
     * Attempts to translate {@code text} into {@code targetLocale}.
     *
     * @param text the source-language text to translate
     * @param targetLocale IETF language tag to translate into, e.g. {@code "he"}
     * @return the translated text, or empty if this provider has no translation available
     *         (not configured, request failed, or timed out) — never throws for that case, so
     *         callers can fall back to the original text without surfacing an error to the user
     */
    Optional<String> translate(String text, String targetLocale);
}
