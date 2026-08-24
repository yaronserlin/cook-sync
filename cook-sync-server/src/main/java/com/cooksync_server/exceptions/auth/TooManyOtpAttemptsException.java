package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when a pending one-time code exceeds the maximum allowed
 * number of incorrect verification attempts. The pending code is invalidated when this occurs,
 * so the caller must request a fresh one to continue. Shared by every OTP-verification flow:
 * registration ({@code AuthServiceImp}), forgot-password reset ({@code PasswordServiceImp}), and
 * self-service email change ({@code UserProfileServiceImp}).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public class TooManyOtpAttemptsException extends RuntimeException {

    /**
     * Constructs a TooManyOtpAttemptsException with context details.
     *
     * @param message exception message context
     */
    public TooManyOtpAttemptsException(String message) {
        super(message);
    }
}
