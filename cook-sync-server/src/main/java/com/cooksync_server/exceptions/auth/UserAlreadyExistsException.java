package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when attempting to register a duplicate email address.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a UserAlreadyExistsException with duplicate email context details.
     *
     * @param message exception message context
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
