package com.cooksync_server.exceptions.auth;

/**
 * Custom runtime exception thrown when a user attempts an operation without sufficient privileges.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public class UnauthorizedActionException extends RuntimeException {

    /**
     * Constructs an UnauthorizedActionException with authorization failure details.
     *
     * @param message failure detail message
     */
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
