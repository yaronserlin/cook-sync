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
     * @param attempt the consecutive-chunk-failure tracker for the top-level translation attempt
     *                this call is part of (e.g. one recipe's whole field set) — implementations
     *                that chunk long text should stop early once {@code attempt} reports too many
     *                consecutive failures, and must not keep their own unscoped failure state
     * @return a {@link TranslationResult}, or empty if this provider has no translation available
     *         at all (not configured, request failed, or timed out) — never throws for that
     *         case, so callers can fall back to the original text without surfacing an error to
     *         the user
     */
    Optional<TranslationResult> translate(String text, String targetLocale, TranslationAttempt attempt);

    /**
     * The outcome of a translate attempt for text that may have needed to be split into multiple
     * chunks (see {@code MyMemoryTranslationProvider}'s 500-byte query cap). A result can be
     * usable ({@code value} is populated) while still incomplete, when only some chunks
     * succeeded — callers should serve an incomplete result to the current request but must not
     * cache it, since the untranslated chunks it contains would otherwise be served forever.
     *
     * @param value the best available translated text — fully translated when {@code complete}
     *              is {@code true}, otherwise a mix of translated and original-language chunks
     * @param complete whether every chunk of {@code text} was successfully translated
     */
    record TranslationResult(String value, boolean complete) {
    }
}
