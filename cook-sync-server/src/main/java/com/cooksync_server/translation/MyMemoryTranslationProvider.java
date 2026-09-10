package com.cooksync_server.translation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link TranslationProvider} backed by the free MyMemory Translation API
 * (https://mymemory.translated.net) — no account/API key required, so this is the
 * {@code @Primary} bean over {@link UnavailableTranslationProvider} unconditionally rather than
 * behind a paid-quota profile switch.
 *
 * <p>The app only ever asks for Hebrew or English (see {@code doc/vision/02-auto-translation.md}
 * — additional languages are explicitly out of scope), so the source language is inferred as
 * "the other one of the pair" rather than threaded through {@link TranslationProvider}'s
 * two-argument signature, which this class does not change.</p>
 *
 * <p>MyMemory caps a single {@code q} query at 500 UTF-8 bytes; recipe instructions/descriptions
 * routinely exceed that (observed up to ~1.5KB in the seed data), so {@link #translate} splits
 * long text into sentence-sized chunks and translates each independently via {@link #chunkTranslator}
 * (a real HTTP call in production, swappable for a fake in tests). Each chunk gets one retry
 * before being considered failed; a chunk that still fails keeps its original-language text in
 * place rather than aborting the whole translation, and the overall result is marked
 * {@link TranslationResult#complete()} {@code false} so callers know not to cache it.</p>
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 05/09/2026
 */
@Slf4j
@Component
@Primary
public class MyMemoryTranslationProvider implements TranslationProvider {

    private static final int TIMEOUT_MS = 4000;
    /** Kept safely under MyMemory's documented 500-byte-per-query cap. */
    private static final int MAX_QUERY_BYTES = 480;
    /** One initial attempt plus one retry before a chunk is considered failed. */
    private static final int MAX_ATTEMPTS_PER_CHUNK = 2;

    private final RestClient client;
    private final ChunkTranslator chunkTranslator;
    private final Counter quotaExhaustedCounter;

    /**
     * Optional contact email sent as MyMemory's {@code de} parameter, which raises the free
     * daily quota from 5,000 to 50,000 characters — no signup, just an address they can reach in
     * case of trouble. Defaults to blank (anonymous, lower-quota) usage.
     */
    @Value("${TRANSLATION_CONTACT_EMAIL:}")
    private String contactEmail;

    @Autowired
    public MyMemoryTranslationProvider(MeterRegistry meterRegistry) {
        this.client = RestClient.builder()
                .baseUrl("https://api.mymemory.translated.net")
                .requestFactory(timeoutRequestFactory())
                .build();
        this.chunkTranslator = this::translateChunkViaHttp;
        this.quotaExhaustedCounter = registerQuotaExhaustedCounter(meterRegistry);
    }

    /**
     * Test-only constructor that swaps the real HTTP call for a caller-supplied
     * {@link ChunkTranslator}, since {@link #client} makes a real network request with no
     * HTTP-mocking dependency available in this project.
     *
     * @param meterRegistry meter registry the quota-exhaustion counter is registered against
     * @param chunkTranslator fake chunk-translation function for the test to control
     */
    MyMemoryTranslationProvider(MeterRegistry meterRegistry, ChunkTranslator chunkTranslator) {
        this.client = null;
        this.chunkTranslator = chunkTranslator;
        this.quotaExhaustedCounter = registerQuotaExhaustedCounter(meterRegistry);
    }

    private static Counter registerQuotaExhaustedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("translation.provider.quota.exhausted")
                .tag("provider", "mymemory")
                .register(meterRegistry);
    }

    @Override
    public Optional<TranslationResult> translate(String text, String targetLocale) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String sourceLocale = inferSourceLocale(targetLocale);
        if (sourceLocale == null) {
            return Optional.empty();
        }

        try {
            StringBuilder result = new StringBuilder();
            boolean anyChunkSucceeded = false;
            boolean everyChunkSucceeded = true;
            for (String piece : chunk(text, MAX_QUERY_BYTES)) {
                String translated = translateChunkWithRetry(piece, sourceLocale, targetLocale);
                if (!result.isEmpty()) {
                    result.append(' ');
                }
                if (translated != null) {
                    result.append(translated);
                    anyChunkSucceeded = true;
                } else {
                    // Keep the chunk's original-language text in place rather than dropping it, so a
                    // partially-translated result still reads as the complete recipe, just mixed-language.
                    result.append(piece);
                    everyChunkSucceeded = false;
                }
            }

            if (!anyChunkSucceeded) {
                return Optional.empty();
            }
            return Optional.of(new TranslationResult(result.toString(), everyChunkSucceeded));
        } catch (RuntimeException e) {
            // Belt-and-braces fallback for anything outside translateChunkWithRetry's own
            // per-chunk try/catch (e.g. a future bug in chunk()'s text processing) — this
            // provider must never surface an exception to the caller, only ever an empty result,
            // matching this codebase's fail-soft translation philosophy (original text beats a
            // broken screen).
            log.warn("MyMemory translation failed unexpectedly (target locale {}): {}", targetLocale, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Translates one chunk, retrying once (transient network hiccups/rate-limit blips are the
     * likely failure mode for a single chunk out of several) before giving up on it.
     *
     * @param text the chunk to translate
     * @param sourceLocale the inferred source language
     * @param targetLocale the requested target language
     * @return the translated chunk, or {@code null} if every attempt failed
     */
    private String translateChunkWithRetry(String text, String sourceLocale, String targetLocale) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_CHUNK; attempt++) {
            try {
                String translated = chunkTranslator.translate(text, sourceLocale, targetLocale);
                if (translated != null) {
                    return translated;
                }
            } catch (RuntimeException e) {
                log.warn("MyMemory chunk translation attempt {}/{} failed (target locale {}): {}",
                        attempt, MAX_ATTEMPTS_PER_CHUNK, targetLocale, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Translates one chunk (already within MyMemory's byte limit) via a single GET request. This
     * is the production {@link ChunkTranslator}; see the test-only constructor for how tests
     * substitute a fake instead of making a real HTTP call.
     *
     * @param text the chunk to translate
     * @param sourceLocale the inferred source language
     * @param targetLocale the requested target language
     * @return the translated chunk, or {@code null} if MyMemory returned no usable result
     */
    private String translateChunkViaHttp(String text, String sourceLocale, String targetLocale) {
        MyMemoryResponse response = client.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/get")
                            .queryParam("q", text)
                            .queryParam("langpair", sourceLocale + "|" + targetLocale);
                    if (StringUtils.hasText(contactEmail)) {
                        uriBuilder.queryParam("de", contactEmail);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(MyMemoryResponse.class);

        if (response == null || response.responseStatus() != 200 || response.responseData() == null
                || !StringUtils.hasText(response.responseData().translatedText())) {
            log.warn("MyMemory returned no usable translation (status={})",
                    response == null ? "null" : response.responseStatus());
            return null;
        }
        if (response.quotaFinished()) {
            quotaExhaustedCounter.increment();
            log.warn("MyMemory's free daily quota is exhausted - translations will fall back to "
                    + "source text until it resets. Set TRANSLATION_CONTACT_EMAIL for a higher quota.");
        }
        return response.responseData().translatedText();
    }

    /**
     * Infers the source language from the requested target, since this app only ever translates
     * between Hebrew and English.
     *
     * @param targetLocale the requested target language tag
     * @return {@code "en"} or {@code "he"}, or {@code null} if {@code targetLocale} is neither
     */
    static String inferSourceLocale(String targetLocale) {
        if (targetLocale == null) {
            return null;
        }
        String normalized = "iw".equalsIgnoreCase(targetLocale) ? "he" : targetLocale.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "he" -> "en";
            case "en" -> "he";
            default -> null;
        };
    }

    /**
     * Splits {@code text} into the fewest ordered chunks whose UTF-8 byte length each stays
     * within {@code maxBytes}, breaking on sentence boundaries where possible and falling back to
     * word boundaries for a single sentence that alone exceeds the budget.
     *
     * @param text the full text to split
     * @param maxBytes the per-chunk byte budget
     * @return the ordered chunks; concatenating them with single spaces reconstructs the text
     *         (modulo whitespace normalization)
     */
    static List<String> chunk(String text, int maxBytes) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : text.split("(?<=[.!?])\\s+")) {
            for (String piece : splitIfTooLong(sentence, maxBytes)) {
                String candidate = current.isEmpty() ? piece : current + " " + piece;
                if (utf8Bytes(candidate) > maxBytes && !current.isEmpty()) {
                    chunks.add(current.toString());
                    current = new StringBuilder(piece);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private static List<String> splitIfTooLong(String sentence, int maxBytes) {
        if (utf8Bytes(sentence) <= maxBytes) {
            return List.of(sentence);
        }
        List<String> words = new ArrayList<>(List.of(sentence.split("\\s+")));
        List<String> pieces = new ArrayList<>();
        StringBuilder piece = new StringBuilder();
        for (String word : words) {
            String candidate = piece.isEmpty() ? word : piece + " " + word;
            if (utf8Bytes(candidate) > maxBytes && !piece.isEmpty()) {
                pieces.add(piece.toString());
                piece = new StringBuilder(word);
            } else {
                piece = new StringBuilder(candidate);
            }
        }
        if (!piece.isEmpty()) {
            pieces.add(piece.toString());
        }
        return pieces;
    }

    private static int utf8Bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return factory;
    }

    /**
     * Seam over the single-chunk HTTP call, extracted so tests can inject a fake instead of
     * hitting the real MyMemory API (no HTTP-mocking dependency exists in this project).
     */
    @FunctionalInterface
    interface ChunkTranslator {
        /**
         * @param text the chunk to translate
         * @param sourceLocale the inferred source language
         * @param targetLocale the requested target language
         * @return the translated chunk, or {@code null} if no usable translation was returned
         */
        String translate(String text, String sourceLocale, String targetLocale);
    }

    /** Shape of MyMemory's JSON response, deserialized for the fields this class actually uses. */
    private record MyMemoryResponse(
            @JsonProperty("responseData") ResponseData responseData,
            @JsonProperty("responseStatus") int responseStatus,
            @JsonProperty("quotaFinished") boolean quotaFinished) {
    }

    private record ResponseData(@JsonProperty("translatedText") String translatedText) {
    }
}
