package com.cooksync_server.exceptions;

/**
 * Custom runtime exception thrown when a requested resource entity cannot be located in the system.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with formatted entity details message.
     *
     * @param resourceName name of target entity resource (e.g. User, Recipe)
     * @param resourceId lookup identifier used in failed query
     */
    public ResourceNotFoundException(String resourceName, String resourceId) {
        super(String.format("%s not found: %s", resourceName, resourceId));
    }
}
