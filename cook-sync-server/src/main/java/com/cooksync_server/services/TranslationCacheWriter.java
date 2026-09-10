package com.cooksync_server.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.repositories.ContentTranslationRepository;
import com.cooksync_server.repositories.TranslationMemoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Persists a newly machine-translated cache row in its own transaction,
 * independent of whatever transaction is already open on the calling thread.
 *
 * <p>
 * {@link TranslationService#resolve} is invoked deep inside response
 * serialization for read endpoints such as
 * {@code RecipeServiceImp#getRecipeById}, which are annotated
 * {@code @Transactional(readOnly = true)}. A plain {@code save()} call from
 * inside {@link TranslationService} would join that ambient read-only
 * transaction (Spring's default {@code REQUIRED} propagation) rather than open
 * its own — and a read-only Hibernate session never flushes pending writes on
 * commit, so the row would silently vanish instead of being persisted. The
 * practical symptom (found the hard way, once a real
 * {@code TranslationProvider} replaced the always-empty
 * {@code UnavailableTranslationProvider}): every view of untranslated content
 * re-calls the external provider from scratch, forever, instead of caching
 * after the first call. {@code REQUIRES_NEW} here forces this specific write
 * into its own transaction that commits on return regardless of the caller's
 * transaction semantics.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@Component
@RequiredArgsConstructor
class TranslationCacheWriter {

    private final ContentTranslationRepository translationRepository;
    private final TranslationMemoryRepository translationMemoryRepository;

    /**
     * Saves a complete, newly machine-translated result in a new, independent transaction: the
     * entity-scoped {@link ContentTranslation} row every mapper reads, plus the shared
     * {@link com.cooksync_server.entities.TranslationMemory} row keyed by the source text's hash
     * so any other entity with identical source text gets a cache hit instead of another provider
     * call.
     *
     * @param translation the entity-scoped row to persist
     * @param sourceTextHash the source text's {@code TranslationMemory} lookup key, as computed
     *                       by {@link TranslationTextHasher#hash} in {@link TranslationService}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void save(ContentTranslation translation, String sourceTextHash) {
        LocalDateTime now = LocalDateTime.now();
        translationRepository.insertIfAbsent(
                UUID.randomUUID().toString(),
                translation.getEntityType().name(),
                translation.getEntityId(),
                translation.getLocale(),
                translation.getValue(),
                translation.getSource().name(),
                now);
        translationMemoryRepository.insertIfAbsent(
                UUID.randomUUID().toString(),
                sourceTextHash,
                translation.getLocale(),
                translation.getValue(),
                translation.getSource().name(),
                now);
    }

    /**
     * Copies a shared {@code TranslationMemory} hit into the entity-scoped cache, in its own
     * transaction for the same reason as {@link #save} — this also runs from inside a read-only
     * GET-endpoint transaction that would otherwise silently drop the write. Does not touch
     * {@code TranslationMemory}, since the value being copied already came from there.
     *
     * @param entityType which field this is
     * @param entityId id of the entity that field belongs to
     * @param locale IETF language tag the translation is in
     * @param value the translated value
     * @param source provenance of the value being copied
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void copyToEntityCache(ContentTranslation.EntityType entityType, String entityId, String locale,
                            String value, ContentTranslation.Source source) {
        translationRepository.insertIfAbsent(
                UUID.randomUUID().toString(),
                entityType.name(),
                entityId,
                locale,
                value,
                source.name(),
                LocalDateTime.now());
    }
}
