package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when a pending registration exceeds the maximum allowed
 * number of incorrect OTP verification attempts. The pending registration is invalidated when
 * this occurs, so the user must submit the registration form again to receive a fresh code.
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
