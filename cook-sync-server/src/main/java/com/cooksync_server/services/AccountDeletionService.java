package com.cooksync_server.services;

import com.cooksync_server.entities.User;

/**
 * Defines the self-service account-deletion lifecycle: starting the 30-day grace period,
 * restoring an account on login within that window, and purging accounts whose grace period
 * has lapsed.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
public interface AccountDeletionService {

    /**
     * Starts the 30-day account-deletion grace period: disables the account, hides its reviews
     * from public view, and revokes active sessions.
     *
     * @param user the account requesting deletion, already password-verified by the caller
     */
    void requestDeletion(User user);

    /**
     * Restores an account to normal: re-enables it, resets its status to active, clears any
     * pending deletion timestamp, and un-hides its reviews.
     *
     * @param user the account being restored
     */
    void restoreFromPendingDeletion(User user);

    /**
     * Permanently purges every account whose 30-day deletion grace period has lapsed without the
     * user logging back in. Intended to be invoked by a scheduled daily trigger.
     */
    void purgeExpiredAccounts();

    /**
     * Permanently purges a single account immediately, regardless of its status or how long ago
     * (if ever) deletion was requested, bypassing the 30-day grace period entirely. Intended for
     * admin-initiated hard deletes, not the self-service or scheduled paths.
     *
     * @param user the account to purge immediately
     */
    void purgeAccountImmediately(User user);
}
