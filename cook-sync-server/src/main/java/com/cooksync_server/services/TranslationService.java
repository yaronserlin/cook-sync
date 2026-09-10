package com.cooksync_server.services;

import java.util.Optional;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.entities.TranslationMemory;
import com.cooksync_server.repositories.ContentTranslationRepository;
import com.cooksync_server.repositories.TranslationMemoryRepository;
import com.cooksync_server.translation.TranslationProvider;
import com.cooksync_server.translation.TranslationProvider.TranslationResult;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

/**
 * Resolves the display value of any translatable field for the current request's locale,
 * backed by {@link ContentTranslationRepository} as a cache/translation-memory and
 * {@link TranslationProvider} for on-demand machine translation on a cache miss. This is the
 * single place the "which language does the requester get" policy lives, so every mapper
 * (recipe title, ingredient name, tag name, ...) applies it identically:
 *
 * <ol>
 *   <li>if the request's locale matches the content's own {@code sourceLocale}, the original
 *       value is already correct — return it untouched;</li>
 *   <li>otherwise look up a cached translation for that field/locale;</li>
 *   <li>on a cache miss, ask {@link #provider} to translate it, caching a successful result as
 *       {@link ContentTranslation.Source#MACHINE} so the same field is never re-translated;</li>
 *   <li>if the provider has nothing (not configured, failed, timed out), fall back to the
 *       original value rather than surfacing an error — a slightly wrong language beats a
 *       broken screen.</li>
 * </ol>
 *
 * <p>Accessed from the (static, dependency-free) mapper classes via {@link TranslationAccess},
 * since converting every mapper into a Spring bean — and every controller/service that calls
 * them statically today — is a much larger refactor than this single field warrants.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/09/2026
 */
@Service
@RequiredArgsConstructor
public class TranslationService {

    private final ContentTranslationRepository translationRepository;
    private final TranslationMemoryRepository translationMemoryRepository;
    private final TranslationProvider provider;
    private final TranslationCacheWriter cacheWriter;
    private final MeterRegistry meterRegistry;

    /**
     * Resolves one field's display value for the current request's locale.
     *
     * @param entityType which field this is (see {@link ContentTranslation.EntityType})
     * @param entityId id of the entity that field belongs to
     * @param original the field's value in {@code sourceLocale}
     * @param sourceLocale the IETF language tag {@code original} is actually written in (e.g.
     *                     a recipe's {@code sourceLocale}, or {@code "en"} for reference data
     *                     like units/tags that are always authored in English)
     * @return the resolved value plus whether it came from on-demand machine translation
     */
    public TranslatedText resolve(ContentTranslation.EntityType entityType, String entityId,
                                   String original, String sourceLocale) {
        String requestLocale = normalize(LocaleContextHolder.getLocale().getLanguage());
        if (requestLocale.equalsIgnoreCase(normalize(sourceLocale)) || original == null || original.isBlank()) {
            return new TranslatedText(original, false);
        }

        Optional<ContentTranslation> cached = translationRepository
                .findByEntityTypeAndEntityIdAndLocale(entityType, entityId, requestLocale);
        resultCounter("translation.cache.lookup", cached.isPresent() ? "hit" : "miss").increment();
        return cached
                .map(c -> new TranslatedText(c.getValue(), c.getSource() == ContentTranslation.Source.MACHINE))
                .orElseGet(() -> translateOnDemand(entityType, entityId, original, requestLocale));
    }

    /**
     * Handles an entity-cache miss: first checks the text-hash-keyed shared
     * {@link TranslationMemory} (a dedup layer independent of which entity {@code original} came
     * from), and only calls {@link #provider} if that also misses. A shared-memory hit is copied
     * into the entity cache so this same entity's next request skips both lookups. A
     * <em>partial</em> provider result (some but not all chunks translated, see
     * {@link TranslationResult#complete()}) is returned to this request but never persisted —
     * caching a permanently mixed-language value would be worse than retrying on the next
     * request.
     */
    private TranslatedText translateOnDemand(ContentTranslation.EntityType entityType, String entityId,
                                              String original, String requestLocale) {
        String textHash = TranslationTextHasher.hash(original);

        Optional<TranslationMemory> shared = translationMemoryRepository.findByTextHashAndLocale(textHash, requestLocale);
        resultCounter("translation.memory.lookup", shared.isPresent() ? "hit" : "miss").increment();
        if (shared.isPresent()) {
            TranslationMemory memory = shared.get();
            cacheWriter.copyToEntityCache(entityType, entityId, requestLocale, memory.getValue(), memory.getSource());
            return new TranslatedText(memory.getValue(), memory.getSource() == ContentTranslation.Source.MACHINE);
        }

        return provider.translate(original, requestLocale)
                .map(result -> {
                    if (result.complete()) {
                        outcomeCounter("success");
                        cacheWriter.save(ContentTranslation.builder()
                                .entityType(entityType)
                                .entityId(entityId)
                                .locale(requestLocale)
                                .value(result.value())
                                .source(ContentTranslation.Source.MACHINE)
                                .build(), textHash);
                    } else {
                        outcomeCounter("partial");
                    }
                    return new TranslatedText(result.value(), true);
                })
                .orElseGet(() -> {
                    outcomeCounter("failure");
                    return new TranslatedText(original, false);
                });
    }

    private Counter resultCounter(String name, String result) {
        return meterRegistry.counter(name, "result", result);
    }

    private void outcomeCounter(String outcome) {
        meterRegistry.counter("translation.provider.calls", "outcome", outcome).increment();
    }

    /**
     * Collapses the legacy {@code iw} language code — how some client runtimes (observed on
     * Android) resolve the device's Hebrew locale internally — onto {@code he}, so a source or
     * request locale expressed either way still matches.
     *
     * @param languageTag a raw IETF language subtag, e.g. from {@code Locale.getLanguage()}
     * @return {@code "he"} for either Hebrew alias, otherwise {@code languageTag} unchanged
     */
    private static String normalize(String languageTag) {
        return "iw".equalsIgnoreCase(languageTag) ? "he" : languageTag;
    }

    /**
     * A resolved display value alongside whether it was produced by on-demand machine
     * translation rather than the entity's own authored text or a human-reviewed seed
     * translation — surfaced to the client so it can show an "auto-translated" indicator rather
     * than presenting machine output with the same confidence as reviewed content.
     *
     * @param value the resolved display value
     * @param isMachineTranslated whether {@code value} came from {@link TranslationProvider} on this or a prior request
     */
    public record TranslatedText(String value, boolean isMachineTranslated) {
    }
}
