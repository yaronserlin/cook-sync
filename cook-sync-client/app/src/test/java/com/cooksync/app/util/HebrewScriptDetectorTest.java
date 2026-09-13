package com.cooksync.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test suite for {@link HebrewScriptDetector}, mirroring cook-sync-server's
 * {@code SourceLocaleDetectorTest} since the two classes implement the same algorithm.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
public class HebrewScriptDetectorTest {

    @Test
    public void isRtl_isFalse_forEnglishText() {
        assertFalse(HebrewScriptDetector.isRtl("Classic Beef Wellington"));
    }

    @Test
    public void isRtl_isTrue_forHebrewText() {
        assertTrue(HebrewScriptDetector.isRtl("שקשוקה עם פטה"));
    }

    @Test
    public void isRtl_isTrue_whenTextIsMixedScript() {
        assertTrue(HebrewScriptDetector.isRtl("Shakshuka שקשוקה"));
    }

    @Test
    public void isRtl_fallsBackToNextSignal_whenFirstSignalIsBlank() {
        assertTrue(HebrewScriptDetector.isRtl("", "מרק עוף"));
    }

    @Test
    public void isRtl_fallsBackToNextSignal_whenFirstSignalIsInconclusive() {
        assertFalse(HebrewScriptDetector.isRtl("2026", "Chicken Soup"));
    }

    @Test
    public void isRtl_isFalse_whenNoSignalIsDecisive() {
        assertFalse(HebrewScriptDetector.isRtl("2026", "", "  "));
    }

    @Test
    public void isRtl_isFalse_forNullOrEmptySignals() {
        assertFalse(HebrewScriptDetector.isRtl((String[]) null));
        assertFalse(HebrewScriptDetector.isRtl());
    }

    @Test
    public void isRtl_stopsAtFirstDecisiveSignal_ignoringLaterOnes() {
        assertFalse(HebrewScriptDetector.isRtl("Chicken Soup", "מרק עוף"));
    }

    @Test
    public void layoutDirection_matchesIsRtl() {
        assertEquals(android.view.View.LAYOUT_DIRECTION_RTL, HebrewScriptDetector.layoutDirection("שקשוקה"));
        assertEquals(android.view.View.LAYOUT_DIRECTION_LTR, HebrewScriptDetector.layoutDirection("Shakshuka"));
    }
}
