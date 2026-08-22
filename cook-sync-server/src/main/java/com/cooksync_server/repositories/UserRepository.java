package com.cooksync_server.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.cooksync_server.entities.User;

/**
 * Spring Data JPA Repository interface for managing User entity persistence.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Retrieves a user entity matching the provided email address.
     *
     * @param email exact email address to search for
     * @return optional containing User if matching account exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifies if a user with the specified email address exists.
     *
     * @param email target email address
     * @return true if email is registered, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Searches users by case-insensitive partial match on first name, last name, or email,
     * optionally filtered by enabled/disabled status.
     *
     * @param q lowercase search fragment, or null to skip name/email filtering
     * @param enabled true/false to filter by account status, or null to include both
     * @param pageable page, size, and sort configuration
     * @return page of matching User entities
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:q IS NULL OR LOWER(u.firstName) LIKE CONCAT('%', :q, '%') " +
            "OR LOWER(u.lastName) LIKE CONCAT('%', :q, '%') " +
            "OR LOWER(u.email) LIKE CONCAT('%', :q, '%')) " +
            "AND (:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> search(@Param("q") String q, @Param("enabled") Boolean enabled, Pageable pageable);

    /**
     * Retrieves every user with an account-deletion request older than the given cutoff, i.e.
     * accounts whose 30-day grace period has lapsed and are due for permanent purge.
     *
     * @param status account status the deletion request left the account in (always {@code DEACTIVATED})
     * @param cutoff purge-eligibility threshold: requests made before this instant qualify
     * @return list of matching User entities
     */
    List<User> findByStatusAndDeletionRequestedAtBefore(User.AccountStatus status, LocalDateTime cutoff);
}
