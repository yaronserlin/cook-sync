package com.cooksync_server.translation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link TranslationAttempt}: the consecutive-chunk-failure trip threshold,
 * the reset on success, and — the regression case this class exists to fix — that two separately
 * created attempts never share failure state.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
class TranslationAttemptTest {

    @Test
    void isTripped_isFalse_forFreshAttempt() {
        assertFalse(TranslationAttempt.create().isTripped());
    }

    @Test
    void isTripped_isFalse_belowThreshold() {
        TranslationAttempt attempt = TranslationAttempt.create();

        attempt.recordFailure();
        attempt.recordFailure();

        assertFalse(attempt.isTripped());
    }

    @Test
    void isTripped_isTrue_atThreshold() {
        TranslationAttempt attempt = TranslationAttempt.create();

        attempt.recordFailure();
        attempt.recordFailure();
        attempt.recordFailure();

        assertTrue(attempt.isTripped());
    }

    @Test
    void recordSuccess_resetsTheConsecutiveCount() {
        TranslationAttempt attempt = TranslationAttempt.create();

        attempt.recordFailure();
        attempt.recordFailure();
        attempt.recordSuccess();
        attempt.recordFailure();
        attempt.recordFailure();

        assertFalse(attempt.isTripped(), "the success should have reset the run of consecutive failures");
    }

    @Test
    void twoSeparatelyCreatedAttempts_doNotShareFailureState() {
        TranslationAttempt first = TranslationAttempt.create();
        TranslationAttempt second = TranslationAttempt.create();

        first.recordFailure();
        first.recordFailure();
        first.recordFailure();

        assertTrue(first.isTripped());
        assertFalse(second.isTripped(), "a fresh attempt must not inherit another attempt's failure count");
    }
}
