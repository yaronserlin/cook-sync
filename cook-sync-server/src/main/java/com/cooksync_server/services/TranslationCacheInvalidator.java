package com.cooksync_server.services;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.repositories.ContentTranslationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Deletes cached {@link ContentTranslation} rows that a recipe edit has made stale or orphaned,
 * so a later request never gets served translated text for content that no longer exists or has
 * since changed.
 *
 * <p>Deliberately runs in the caller's own transaction (default propagation) rather than
 * {@link TranslationCacheWriter}'s {@code REQUIRES_NEW} — the opposite tradeoff is correct here:
 * if the surrounding recipe edit rolls back, this invalidation must roll back with it, or a
 * failed edit would still silently delete translations for content that was never actually
 * changed.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
@Component
@RequiredArgsConstructor
class TranslationCacheInvalidator {

    private final ContentTranslationRepository translationRepository;

    /**
     * Invalidates every cached translation for one field of one entity.
     *
     * @param entityType the translatable field
     * @param entityId id of the entity that field belongs to, or {@code null}/blank to no-op
     */
    void invalidate(ContentTranslation.EntityType entityType, String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return;
        }
        translationRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Invalidates every cached translation for one field across many entities at once (e.g. the
     * ingredient names replaced by a recipe edit).
     *
     * @param entityType the translatable field
     * @param entityIds ids of the entities that field belongs to; a null/empty collection no-ops
     */
    void invalidateAll(ContentTranslation.EntityType entityType, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return;
        }
        translationRepository.deleteByEntityTypeAndEntityIdIn(entityType, entityIds);
    }
}
