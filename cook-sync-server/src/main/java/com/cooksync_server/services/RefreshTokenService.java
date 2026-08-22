package com.cooksync_server.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.entities.RefreshToken;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.RefreshTokenRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing user session refresh token generation, validation, and deletion.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements IRefreshTokenService{

    @Value("${jwt.refreshExpirationMs:604800000}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Finds a RefreshToken entity by string value.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param token refresh token string
     * @return Optional containing RefreshToken if found
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Generates a new RefreshToken entity for specified user, revoking any existing user session token.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param userId unique user identifier
     * @return created RefreshToken entity
     */
    @Transactional
    public RefreshToken createRefreshToken(String userId) {
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", userId)))
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verifies that a RefreshToken has not expired, deleting it if expired.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param token target RefreshToken entity
     * @return valid RefreshToken instance
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedActionException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    /**
     * Deletes all refresh tokens belonging to a user ID.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param userId target user ID
     */
    @Transactional
    public void deleteByUserId(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}