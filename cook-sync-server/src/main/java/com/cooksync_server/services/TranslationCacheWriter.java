package com.cooksync_server.services;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.repositories.ContentTranslationRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Persists a newly machine-translated cache row in its own transaction, independent of whatever
 * transaction is already open on the calling thread.
 *
 * <p>{@link TranslationService#resolve} is invoked deep inside response serialization for read
 * endpoints such as {@code RecipeServiceImp#getRecipeById}, which are annotated
 * {@code @Transactional(readOnly = true)}. A plain {@code save()} call from inside
 * {@link TranslationService} would join that ambient read-only transaction (Spring's default
 * {@code REQUIRED} propagation) rather than open its own — and a read-only Hibernate session
 * never flushes pending writes on commit, so the row would silently vanish instead of being
 * persisted. The practical symptom (found the hard way, once a real {@code TranslationProvider}
 * replaced the always-empty {@code UnavailableTranslationProvider}): every view of untranslated
 * content re-calls the external provider from scratch, forever, instead of caching after the
 * first call. {@code REQUIRES_NEW} here forces this specific write into its own transaction that
 * commits on return regardless of the caller's transaction semantics.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@Component
@RequiredArgsConstructor
class TranslationCacheWriter {

    private final ContentTranslationRepository translationRepository;

    /**
     * Saves a machine-translated cache row in a new, independent transaction.
     *
     * @param translation the row to persist
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void save(ContentTranslation translation) {
        translationRepository.save(translation);
    }
}
