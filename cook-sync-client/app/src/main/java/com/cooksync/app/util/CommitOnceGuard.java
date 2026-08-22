package com.cooksync.app.util;

/**
 * Guards against a duplicate commit when two UI events fire for the same user gesture — e.g. an
 * explicit save-icon tap and the focus-loss it triggers on the field being saved. Call
 * {@link #reset()} when an editor opens, and {@link #tryCommit()} at the start of each commit
 * path; only the first call after a reset actually proceeds.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 22/08/2026
 */
public final class CommitOnceGuard {

    private boolean committed = false;

    /**
     * Resets the guard so the next {@link #tryCommit()} call succeeds again.
     */
    public void reset() {
        committed = false;
    }

    /**
     * Attempts to commit, succeeding only the first time this is called since the last
     * {@link #reset()}.
     *
     * @return {@code true} if this call may proceed with the commit; {@code false} if a commit
     *         already went through since the last reset
     */
    public boolean tryCommit() {
        if (committed) {
            return false;
        }
        committed = true;
        return true;
    }
}
