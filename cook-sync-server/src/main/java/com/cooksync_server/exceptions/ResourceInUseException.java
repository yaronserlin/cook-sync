package com.cooksync_server.exceptions;

/**
 * Custom runtime exception thrown when deleting an entity that is still referenced by other entities.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 31/08/2026
 */
public class ResourceInUseException extends RuntimeException {

    /**
     * Constructs a ResourceInUseException describing why the resource cannot be deleted.
     *
     * @param resourceName name of the resource entity blocked from deletion
     * @param resourceId unique identifier of the target resource
     * @param reason description of what is still referencing the resource
     */
    public ResourceInUseException(String resourceName, String resourceId, String reason) {
        super(String.format("%s %s cannot be deleted: %s", resourceName, resourceId, reason));
    }
}
