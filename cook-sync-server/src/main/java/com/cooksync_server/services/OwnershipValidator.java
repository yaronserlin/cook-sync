package com.cooksync_server.services;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.UserRepository;

/**
 * Utility validator providing resource ownership and administrator authorization checks.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
final class OwnershipValidator {

    private OwnershipValidator() {
    }

    /**
     * Verifies that the current authenticated user is either the owner of the target resource or an administrator.
     *
     * @param ownerId unique user identifier of the resource creator
     * @param currentUser authenticated user attempting the mutation
     * @param errorMessage detail exception message thrown upon authorization failure
     * @throws UnauthorizedActionException if user is neither owner nor administrator
     */
    static void requireOwnerOrAdmin(String ownerId, User currentUser, String errorMessage) {
        if (!ownerId.equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedActionException(errorMessage);
        }
    }

    /**
     * Looks up a target resource and the acting user, then verifies the resource is owned by
     * that user or that the user is an administrator, before returning the resource. Consolidates
     * the "load resource, load user, authorize" sequence otherwise repeated across the recipe,
     * ingredient, instruction, and review services.
     *
     * @param <T> resource entity type
     * @param resourceLookup supplier resolving the target resource, empty if not found
     * @param resourceName resource type label used in the not-found exception
     * @param resourceId resource identifier used in the not-found exception
     * @param ownerIdExtractor extracts the owning user's ID from the resolved resource
     * @param userRepository repository used to resolve the acting user by email
     * @param userEmail email address of the user attempting the mutation
     * @param errorMessage detail exception message thrown upon authorization failure
     * @return the resolved resource, once ownership has been verified
     * @throws ResourceNotFoundException if the resource or acting user cannot be found
     * @throws UnauthorizedActionException if the user is neither owner nor administrator
     */
    static <T> T requireOwnedResource(Supplier<Optional<T>> resourceLookup, String resourceName, String resourceId,
            Function<T, String> ownerIdExtractor, UserRepository userRepository, String userEmail,
            String errorMessage) {
        T resource = resourceLookup.get()
                .orElseThrow(() -> new ResourceNotFoundException(resourceName, resourceId));
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));
        requireOwnerOrAdmin(ownerIdExtractor.apply(resource), currentUser, errorMessage);
        return resource;
    }
}
