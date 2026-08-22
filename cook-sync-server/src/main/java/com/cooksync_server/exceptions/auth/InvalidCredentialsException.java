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
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param message descriptive exception message
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }

    /**
     * Constructs an InvalidCredentialsException with default error message.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     */
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
