package com.cooksync_server.exceptions;

/**
 * Custom runtime exception thrown when creating an entity that violates duplicate resource constraints.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a ResourceAlreadyExistsException with resource name and key.
     *
     * @param resourceName name of the conflicting resource entity
     * @param resourceId unique identifier string causing the duplication conflict
     */
    public ResourceAlreadyExistsException(String resourceName, String resourceId) {
        super(String.format("%s already exists: %s", resourceName, resourceId));
    }
}
