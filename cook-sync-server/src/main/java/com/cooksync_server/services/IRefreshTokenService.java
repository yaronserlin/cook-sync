package com.cooksync_server.services;

import java.util.Optional;

import com.cooksync_server.entities.RefreshToken;

/**
 * Service interface for issuing, validating, rotating, and revoking session refresh tokens.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface IRefreshTokenService {

    /**
     * Finds a RefreshToken entity by its token string value.
     *
     * @param token refresh token string
     * @return optional containing the RefreshToken if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Issues a new RefreshToken for the specified user, revoking any existing token for that user.
     *
     * @param userId unique user identifier
     * @return the created RefreshToken entity
     */
    RefreshToken createRefreshToken(String userId);

    /**
     * Verifies that a RefreshToken has not expired, deleting it if it has.
     *
     * @param token target RefreshToken entity
     * @return the same RefreshToken instance if still valid
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the token has expired
     */
    RefreshToken verifyExpiration(RefreshToken token);

    /**
     * Deletes all refresh tokens belonging to a user ID.
     *
     * @param userId target user ID
     */
    void deleteByUserId(String userId);
}
