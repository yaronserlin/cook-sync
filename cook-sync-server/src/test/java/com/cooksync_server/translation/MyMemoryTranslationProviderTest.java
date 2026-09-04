package com.cooksync_server.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link MyMemoryTranslationProvider}'s pure logic: inferring the source
 * language from the app's Hebrew/English-only target locales, and splitting text that exceeds
 * MyMemory's 500-byte-per-query cap into chunks that each stay within budget. Does not exercise
 * {@link MyMemoryTranslationProvider#translate} itself, which makes a real HTTP call.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
class MyMemoryTranslationProviderTest {

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
