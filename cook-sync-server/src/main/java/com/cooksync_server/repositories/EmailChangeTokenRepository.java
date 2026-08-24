package com.cooksync_server.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.EmailChangeToken;

/**
 * Spring Data JPA Repository interface for EmailChangeToken entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
@Repository
public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, String> {

    /**
     * Finds the active EmailChangeToken row for a specific user, if one exists (at most one per
     * user at a time, since a fresh email-change request deletes any prior row first).
     *
     * @param userId unique user identifier
     * @return optional containing EmailChangeToken if located
     */
    Optional<EmailChangeToken> findByUserId(String userId);

    /**
     * Deletes all outstanding email-change tokens for a specific user ID, so a fresh
     * email-change request invalidates any earlier unused token for the same account.
     *
     * @param userId unique user identifier
     */
    @Modifying
    @Query("DELETE FROM EmailChangeToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
