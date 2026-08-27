package com.cooksync_server.constants;

/**
 * Centralizes time-window constants for one-time verification codes and the self-service
 * account-deletion grace period, previously declared independently (but identically) in several
 * service classes.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class VerificationWindows {

    private VerificationWindows() {
    }

    /**
     * How many minutes a one-time verification code (registration OTP, password-reset code, or
     * email-change code) remains valid after being issued or resent.
     */
    public static final int CODE_VALIDITY_MINUTES = 10;

    /** {@link #CODE_VALIDITY_MINUTES} expressed in milliseconds, for {@link java.time.Instant} arithmetic. */
    public static final long CODE_VALIDITY_MS = CODE_VALIDITY_MINUTES * 60 * 1000L;

    /**
     * Number of days between a self-service account-deletion request and either permanent purge
     * (if the user never logs back in) or automatic restoration (if they do).
     */
    public static final long ACCOUNT_DELETION_GRACE_PERIOD_DAYS = 30;
}
