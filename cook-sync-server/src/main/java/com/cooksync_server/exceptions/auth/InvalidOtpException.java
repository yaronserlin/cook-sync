package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when a submitted one-time code does not match the expected
 * code, or no code is currently pending for the target account. Shared by every OTP-verification
 * flow: registration ({@code AuthServiceImp}), forgot-password reset ({@code PasswordServiceImp}),
 * and self-service email change ({@code UserProfileServiceImp}).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public class InvalidOtpException extends RuntimeException {

    /**
     * Constructs an InvalidOtpException with context details.
     *
     * @param message exception message context
     */
    public InvalidOtpException(String message) {
        super(message);
    }
}
