package com.cooksync_server.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.cooksync_server.translation.TranslationProvider.TranslationResult;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit test suite for {@link MyMemoryTranslationProvider}: the pure logic (inferring the source
 * language from the app's Hebrew/English-only target locales, splitting text that exceeds
 * MyMemory's 500-byte-per-query cap into chunks that each stay within budget) plus
 * {@link MyMemoryTranslationProvider#translate}'s retry/partial-failure behavior, exercised
 * against a fake {@link MyMemoryTranslationProvider.ChunkTranslator} rather than a real HTTP call
 * (no HTTP-mocking dependency exists in this project).
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 05/09/2026
 */
class MyMemoryTranslationProviderTest {

    private MyMemoryTranslationProvider providerWith(MyMemoryTranslationProvider.ChunkTranslator chunkTranslator) {
        return new MyMemoryTranslationProvider(new SimpleMeterRegistry(), chunkTranslator);
    }

    @Test
    void translate_returnsComplete_whenAllChunksSucceed() {
        MyMemoryTranslationProvider provider = providerWith((text, source, target) -> text + "-translated");

        Optional<TranslationResult> result = provider.translate("hello", "he");

        assertTrue(result.isPresent());
        assertEquals("hello-translated", result.get().value());
        assertTrue(result.get().complete());
    }

    @Test
    void translate_retriesOnce_beforeGivingUpOnAChunk() {
        AtomicInteger callCount = new AtomicInteger();
        MyMemoryTranslationProvider provider = providerWith((text, source, target) -> {
            if (callCount.getAndIncrement() == 0) {
                return null;
            }
            return "translated";
        });

        Optional<TranslationResult> result = provider.translate("hello", "he");

        assertTrue(result.isPresent());
        assertEquals("translated", result.get().value());
        assertTrue(result.get().complete());
        assertEquals(2, callCount.get());
    }

    @Test
    void translate_retriesOnce_afterAnException_beforeGivingUpOnAChunk() {
        AtomicInteger callCount = new AtomicInteger();
        MyMemoryTranslationProvider provider = providerWith((text, source, target) -> {
            if (callCount.getAndIncrement() == 0) {
                throw new RuntimeException("connection reset");
            }
            return "translated";
        });

        Optional<TranslationResult> result = provider.translate("hello", "he");

        assertTrue(result.isPresent());
        assertEquals("translated", result.get().value());
        assertTrue(result.get().complete());
    }

    @Test
    void translate_returnsIncompletePartialResult_whenOneOfMultipleChunksStillFailsAfterRetry() {
        // Two sentences, each individually under the 480-byte budget but combined over it, so
        // chunk() is guaranteed to split them into exactly two chunks (verified below) rather
        // than merging them into one, which a short two-sentence input would do instead.
        String sentenceOne = "Alpha word ".repeat(28).trim() + " end.";
        String sentenceTwo = "Beta word ".repeat(28).trim() + " end.";
        String text = sentenceOne + " " + sentenceTwo;
        assertEquals(2, MyMemoryTranslationProvider.chunk(text, 480).size(),
                "test setup expects exactly two chunks; adjust sentence length if this fails");

        MyMemoryTranslationProvider provider = providerWith((chunk, source, target) ->
                chunk.contains("Alpha") ? "ALPHA-TRANSLATED" : null);

        Optional<TranslationResult> result = provider.translate(text, "he");

        assertTrue(result.isPresent());
        assertFalse(result.get().complete());
        assertTrue(result.get().value().contains("ALPHA-TRANSLATED"));
        assertTrue(result.get().value().contains(sentenceTwo),
                "the still-failing chunk should keep its original text rather than being dropped");
    }

    @Test
    void translate_returnsEmpty_whenEveryChunkFails() {
        MyMemoryTranslationProvider provider = providerWith((text, source, target) -> null);

        Optional<TranslationResult> result = provider.translate("hello", "he");

        assertTrue(result.isEmpty());
    }

    @Test
    void translate_returnsEmpty_forBlankText() {
        MyMemoryTranslationProvider provider = providerWith((text, source, target) -> "unused");

        assertTrue(provider.translate("", "he").isEmpty());
        assertTrue(provider.translate(null, "he").isEmpty());
    }

    @Test
    void translate_returnsEmpty_forUnsupportedTargetLocale() {
        MyMemoryTranslationProvider provider = providerWith((text, source, target) -> "unused");

        assertTrue(provider.translate("hello", "fr").isEmpty());
    }

    @Test
    void inferSourceLocale_returnsEnglish_forHebrewTarget() {
        assertEquals("en", MyMemoryTranslationProvider.inferSourceLocale("he"));
    }

    @Test
    void inferSourceLocale_returnsHebrew_forEnglishTarget() {
        assertEquals("he", MyMemoryTranslationProvider.inferSourceLocale("en"));
    }

    @Test
    void inferSourceLocale_treatsLegacyIwAlias_asHebrew() {
        assertEquals("en", MyMemoryTranslationProvider.inferSourceLocale("iw"));
    }

    @Test
    void inferSourceLocale_returnsNull_forUnsupportedLanguage() {
        assertNull(MyMemoryTranslationProvider.inferSourceLocale("fr"));
    }

    @Test
    void inferSourceLocale_returnsNull_forNullInput() {
        assertNull(MyMemoryTranslationProvider.inferSourceLocale(null));
    }

    @Test
    void chunk_returnsSingleChunk_whenTextIsWithinBudget() {
        List<String> chunks = MyMemoryTranslationProvider.chunk("A short sentence.", 480);
        assertEquals(1, chunks.size());
        assertEquals("A short sentence.", chunks.get(0));
    }

    @Test
    void chunk_splitsOnSentenceBoundaries_whenTextExceedsBudget() {
        String sentence = "The quick brown fox jumps over the lazy dog. ".repeat(1);
        String text = sentence.repeat(20);

        List<String> chunks = MyMemoryTranslationProvider.chunk(text, 100);

        assertTrue(chunks.size() > 1, "expected the long text to be split into multiple chunks");
        for (String piece : chunks) {
            assertTrue(piece.getBytes(StandardCharsets.UTF_8).length <= 100,
                    "chunk exceeded the byte budget: " + piece);
        }
        // No sentence content is dropped.
        String rejoined = String.join(" ", chunks);
        assertEquals(text.replaceAll("\\s+", " ").trim(), rejoined.replaceAll("\\s+", " ").trim());
    }

    @Test
    void chunk_fallsBackToWordSplitting_forOneOverlongSentenceWithNoPunctuation() {
        String longRun = "word ".repeat(100).trim();

        List<String> chunks = MyMemoryTranslationProvider.chunk(longRun, 50);

        assertTrue(chunks.size() > 1);
        for (String piece : chunks) {
            assertTrue(piece.getBytes(StandardCharsets.UTF_8).length <= 50,
                    "chunk exceeded the byte budget: " + piece);
        }
    }

    @Test
    void chunk_respectsByteBudget_forMultiByteHebrewText() {
        // Each Hebrew letter is 2 bytes in UTF-8, so a naive character-count-based split would
        // silently blow past a byte-based limit.
        String hebrewSentence = "שקשוקה עם פטה, רוקט, פסטו ופיסטוקים היא מנה טעימה ומהירה להכנה. ".repeat(6);

        List<String> chunks = MyMemoryTranslationProvider.chunk(hebrewSentence, 480);

        assertTrue(chunks.size() > 1);
        for (String piece : chunks) {
            assertTrue(piece.getBytes(StandardCharsets.UTF_8).length <= 480,
                    "chunk exceeded the byte budget: " + piece.getBytes(StandardCharsets.UTF_8).length);
        }
    }
}
