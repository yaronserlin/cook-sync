package com.cooksync_server.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link SourceLocaleDetector}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
class SourceLocaleDetectorTest {

    @Test
    void detect_returnsEnglish_forEnglishText() {
        assertEquals("en", SourceLocaleDetector.detect("Classic Beef Wellington"));
    }

    @Test
    void detect_returnsHebrew_forHebrewText() {
        assertEquals("he", SourceLocaleDetector.detect("שקשוקה עם פטה"));
    }

    @Test
    void detect_returnsHebrew_whenTextIsMixedScript() {
        assertEquals("he", SourceLocaleDetector.detect("Shakshuka שקשוקה"));
    }

    @Test
    void detect_fallsBackToNextSignal_whenFirstSignalIsBlank() {
        assertEquals("he", SourceLocaleDetector.detect("", "מרק עוף"));
    }

    @Test
    void detect_fallsBackToNextSignal_whenFirstSignalIsInconclusive() {
        assertEquals("en", SourceLocaleDetector.detect("2026", "Chicken Soup"));
    }

    @Test
    void detect_returnsEnglishDefault_whenNoSignalIsDecisive() {
        assertEquals("en", SourceLocaleDetector.detect("2026", "", "  "));
    }

    @Test
    void detect_returnsEnglishDefault_forNullOrEmptySignals() {
        assertEquals("en", SourceLocaleDetector.detect((List<String>) null));
        assertEquals("en", SourceLocaleDetector.detect(List.of()));
    }

    @Test
    void detect_stopsAtFirstDecisiveSignal_ignoringLaterOnes() {
        assertEquals("en", SourceLocaleDetector.detect("Chicken Soup", "מרק עוף"));
    }
}
