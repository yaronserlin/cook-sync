package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link TranslationTextHasher}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
class TranslationTextHasherTest {

    @Test
    void hash_isDeterministic_forTheSameText() {
        assertEquals(TranslationTextHasher.hash("Flour"), TranslationTextHasher.hash("Flour"));
    }

    @Test
    void hash_isStable_acrossSurroundingWhitespaceDifferences() {
        assertEquals(TranslationTextHasher.hash("Flour"), TranslationTextHasher.hash("  Flour  "));
    }

    @Test
    void hash_isStable_acrossInternalWhitespaceRunDifferences() {
        assertEquals(TranslationTextHasher.hash("2 cups flour"), TranslationTextHasher.hash("2   cups\tflour"));
    }

    @Test
    void hash_isCaseSensitive() {
        assertNotEquals(TranslationTextHasher.hash("flour"), TranslationTextHasher.hash("Flour"));
    }

    @Test
    void hash_differsForDifferentText() {
        assertNotEquals(TranslationTextHasher.hash("flour"), TranslationTextHasher.hash("sugar"));
    }

    @Test
    void hash_returnsSixtyFourCharacterHex() {
        String hash = TranslationTextHasher.hash("Flour");
        assertEquals(64, hash.length());
        assertEquals(hash, hash.toLowerCase(java.util.Locale.ROOT));
    }
}
