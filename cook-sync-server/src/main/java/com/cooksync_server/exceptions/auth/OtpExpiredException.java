package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when a submitted one-time code has expired. Shared by every
 * OTP-verification flow: registration ({@code AuthServiceImp}), forgot-password reset
 * ({@code PasswordServiceImp}), and self-service email change ({@code UserProfileServiceImp}).
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
