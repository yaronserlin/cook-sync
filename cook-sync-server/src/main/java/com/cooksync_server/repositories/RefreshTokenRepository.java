package com.cooksync_server.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.RefreshToken;

/**
 * Spring Data JPA Repository interface for RefreshToken entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Finds a RefreshToken entity by its unique token string.
     *
     * @param token refresh token string value
     * @return optional containing RefreshToken if located
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens registered to a specific user ID.
     *
     * @param userId unique user identifier
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
