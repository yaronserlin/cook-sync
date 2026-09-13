package com.cooksync_server.translation;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks consecutive chunk-translation failures for a single translation attempt — one top-level
 * translation call chain, such as resolving every field of one recipe — so a provider can stop
 * retrying once it is clearly failing without that state leaking into any other concurrent or
 * later request. Replaces a previous design where this counter lived as a singleton instance
 * field on {@link MyMemoryTranslationProvider}, shared unscoped across the entire running server
 * process with no reset once tripped.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
public final class TranslationAttempt {

    /**
     * Stop spending requests once a provider is failing consistently within this attempt.
     */
    private static final int MAX_CONSECUTIVE_FAILED_CHUNKS = 3;

    private final AtomicInteger consecutiveFailedChunks = new AtomicInteger();

    private TranslationAttempt() {
    }

    /**
     * @return a fresh attempt with no recorded failures
     */
    public static TranslationAttempt create() {
        return new TranslationAttempt();
    }

    /**
     * @return whether this attempt has already accumulated
     * {@value #MAX_CONSECUTIVE_FAILED_CHUNKS} consecutive chunk failures and should stop making
     * further chunk requests
     */
    boolean isTripped() {
        return consecutiveFailedChunks.get() >= MAX_CONSECUTIVE_FAILED_CHUNKS;
    }

    /**
     * Resets the consecutive-failure count after a chunk succeeds.
     */
    void recordSuccess() {
        consecutiveFailedChunks.set(0);
    }

    /**
     * Records one more consecutive chunk failure.
     *
     * @return the updated consecutive-failure count
     */
    int recordFailure() {
        return consecutiveFailedChunks.incrementAndGet();
    }
}
