package com.cooksync_server.services;

import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;

/**
 * Utility validator providing resource ownership and administrator authorization checks.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
final class OwnershipValidator {

    private OwnershipValidator() {
    }

    /**
     * Verifies that the current authenticated user is either the owner of the target resource or an administrator.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
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
}
