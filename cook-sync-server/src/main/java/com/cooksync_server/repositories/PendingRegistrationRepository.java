package com.cooksync_server.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.PendingRegistration;

/**
 * Spring Data JPA Repository interface for PendingRegistration entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {

    /**
     * Finds a pending registration by its target email address.
     *
     * @param email target email address
     * @return optional containing PendingRegistration if a pending signup exists
     */
    Optional<PendingRegistration> findByEmail(String email);

    /**
     * Deletes any pending registration for a specific email, so a fresh registration attempt
     * for the same address invalidates an earlier unverified one.
     *
     * @param email target email address
     */
    @Modifying
    @Query("DELETE FROM PendingRegistration p WHERE p.email = :email")
    void deleteByEmail(@Param("email") String email);

    /**
     * Deletes every pending registration whose OTP expired before the given cutoff, used by the
     * scheduled cleanup job to purge abandoned registration attempts.
     *
     * @param cutoff purge-eligibility threshold: expirations before this instant qualify
     */
    @Modifying
    @Query("DELETE FROM PendingRegistration p WHERE p.otpExpiresAt < :cutoff")
    void deleteByOtpExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
