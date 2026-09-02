package com.cooksync_server.translation;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Default {@link TranslationProvider}: no external machine-translation backend is configured
 * (no API key/account has been provisioned), so it always reports "no translation available"
 * rather than calling anything. This keeps the on-demand-translation path fully wired end to
 * end — {@link com.cooksync_server.services.TranslationService} still checks the cache, calls
 * this provider, and falls back to the original text exactly as it will once a real provider is
 * plugged in — without the project taking on a paid external dependency before one is chosen.
 *
 * <p>Replace this class (or add a {@code @Primary}/{@code @Profile}-qualified alternative) with
 * a real backend — e.g. Google Cloud Translation or Azure Translator — when one is provisioned;
 * nothing else in the translation path needs to change.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/09/2026
 */
@Component
public class UnavailableTranslationProvider implements TranslationProvider {

    @Override
    public Optional<String> translate(String text, String targetLocale) {
        return Optional.empty();
    }
}
