package com.cooksync_server.exceptions;

/**
 * Custom runtime exception thrown when creating an entity that violates duplicate resource constraints.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public class ResourceAllReadyExistsException extends RuntimeException {

    /**
     * Constructs a ResourceAllReadyExistsException with resource name and key.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param resourceName name of the conflicting resource entity
     * @param resourceId unique identifier string causing the duplication conflict
     */
    public ResourceAllReadyExistsException(String resourceName, String resourceId) {
        super(String.format("%s already exists: %s", resourceName, resourceId));
    }
}
