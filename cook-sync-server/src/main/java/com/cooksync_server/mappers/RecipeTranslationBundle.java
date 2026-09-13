package com.cooksync_server.mappers;

import java.util.HashMap;
import java.util.Map;

import com.cooksync_server.entities.ContentTranslation.EntityType;
import com.cooksync_server.services.TranslationService.TranslatedText;

/**
 * Holds every translatable field resolved by one {@link RecipeTranslationCoordinator} attempt,
 * plus the all-or-nothing verdict for that attempt: if every field completed translation (or
 * didn't need it, because the request locale already matched the source), {@link #valueOf}
 * returns each field's resolved value; if even one field fell back after translation was
 * attempted, {@link #valueOf} returns every field's original value instead, so a recipe is never
 * shown in a mix of languages.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
final class RecipeTranslationBundle {

    private final Map<String, TranslatedText> resolved = new HashMap<>();
    private boolean allComplete = true;
    private boolean anyMachineTranslated = false;

    /**
     * Records one field's resolution outcome into this bundle.
     *
     * @param type which field this is
     * @param entityId id of the entity that field belongs to
     * @param resolvedText the field's resolution outcome
     */
    void record(EntityType type, String entityId, TranslatedText resolvedText) {
        resolved.put(key(type, entityId), resolvedText);
        if (resolvedText.fellBack()) {
            allComplete = false;
        }
        if (resolvedText.isMachineTranslated()) {
            anyMachineTranslated = true;
        }
    }

    /**
     * @param type which field this is
     * @param entityId id of the entity that field belongs to
     * @param originalFallback the field's original-locale value, returned whenever this bundle's
     *                         overall attempt did not complete (or this field was never recorded)
     * @return the resolved (possibly translated) value if every field recorded in this bundle
     *         completed translation, otherwise {@code originalFallback}
     */
    String valueOf(EntityType type, String entityId, String originalFallback) {
        if (!allComplete) {
            return originalFallback;
        }
        TranslatedText text = resolved.get(key(type, entityId));
        return text == null ? originalFallback : text.value();
    }

    /**
     * @return whether the recipe this bundle covers should be presented as machine-translated —
     *         {@code false} whenever any field fell back, otherwise {@code true} iff at least one
     *         recorded field was actually machine-translated (as opposed to already matching the
     *         request locale)
     */
    boolean isMachineTranslated() {
        return allComplete && anyMachineTranslated;
    }

    private static String key(EntityType type, String entityId) {
        return type.name() + ":" + entityId;
    }
}
