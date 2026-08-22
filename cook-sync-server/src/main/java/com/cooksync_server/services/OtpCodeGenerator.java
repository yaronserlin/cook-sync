package com.cooksync_server.services;

import java.security.SecureRandom;

/**
 * Utility generating random 6-digit numeric one-time codes, shared by every OTP-style flow
 * (registration verification, password reset) so the generation logic and shared attempt-limit
 * constant live in exactly one place.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
final class OtpCodeGenerator {

    /** Maximum incorrect OTP submissions allowed before a pending code is invalidated. */
    static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OtpCodeGenerator() {
    }

    /**
     * Generates a random 6-digit numeric OTP code, zero-padded so every code is exactly 6
     * characters (e.g. "004821").
     *
     * @return a 6-digit numeric OTP code string
     */
    static String generate() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
