package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when a submitted registration OTP code does not match the
 * expected code, or no pending registration exists for the given email.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public class InvalidOtpException extends RuntimeException {

    /**
     * Constructs an InvalidOtpException with context details.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param message exception message context
     */
    public InvalidOtpException(String message) {
        super(message);
    }
}
