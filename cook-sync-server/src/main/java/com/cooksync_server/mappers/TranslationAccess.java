package com.cooksync_server.mappers;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.services.TranslationService;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Publishes the singleton {@link TranslationService} bean to a static field so every mapper in
 * this package — all static utility classes with no constructor to inject a dependency into,
 * called from services/controllers as plain static methods — can resolve a translated field
 * without every one of those call sites (and the mappers' own static cross-calls to each other)
 * being refactored into Spring-managed, constructor-injected beans for this one dependency.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/09/2026
 */
@Component
class TranslationAccess {

    private static volatile TranslationService instance;

    private final TranslationService translationService;

    TranslationAccess(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostConstruct
    void publish() {
        instance = translationService;
    }

    /**
     * Resolves one field's display value for the current request's locale. See
     * {@link TranslationService#resolve}.
     *
     * <p>Falls back to returning {@code original} untranslated if no Spring context has
     * published a {@link TranslationService} yet (e.g. a plain Mockito unit test that
     * constructs entities/mappers directly without bootstrapping the application context)
     * rather than throwing — the mappers stay usable in that setting exactly as they were
     * before translation support existed.</p>
     *
     * @param entityType which field this is
     * @param entityId id of the entity that field belongs to
     * @param original the field's value in {@code sourceLocale}
     * @param sourceLocale the IETF language tag {@code original} is actually written in
     * @return the resolved value plus whether it came from on-demand machine translation
     */
    static TranslationService.TranslatedText resolve(ContentTranslation.EntityType entityType, String entityId,
                                                       String original, String sourceLocale) {
        if (instance == null) {
            return new TranslationService.TranslatedText(original, false);
        }
        return instance.resolve(entityType, entityId, original, sourceLocale);
    }
}
