package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when user authentication fails due to incorrect credentials.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Constructs an InvalidCredentialsException with a detailed failure message.
     *
     * @param message descriptive exception message
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }

    /**
     * Constructs an InvalidCredentialsException with default error message.
     */
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
