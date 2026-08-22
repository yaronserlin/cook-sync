package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when a submitted registration OTP code has expired.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public class OtpExpiredException extends RuntimeException {

    /**
     * Constructs an OtpExpiredException with context details.
     *
     * @param message exception message context
     */
    public OtpExpiredException(String message) {
        super(message);
    }
}
