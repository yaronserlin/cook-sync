package com.cooksync_server.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.cooksync_server.entities.User;

/**
 * Spring Data JPA repository managing persistence for the {@link User} entity.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Retrieves the user entity matching the given email address.
     *
     * @param email exact email address to search for
     * @return an optional containing the matching User, or empty if no account has that email
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email address is already registered.
     *
     * @param email target email address
     * @return {@code true} if the email is registered, {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Searches users by case-insensitive partial match on first name, last name, or email,
     * optionally filtered by enabled/disabled status.
     *
     * @param q lowercase search fragment, or {@code null} to skip the name/email filter
     * @param enabled {@code true} or {@code false} to filter by account status, or {@code null} to include both
     * @param pageable page, size, and sort configuration
     * @return a page of matching User entities
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
     * @param cutoff purge-eligibility threshold; requests made before this instant qualify
     * @return the list of matching User entities
     */
    List<User> findByStatusAndDeletionRequestedAtBefore(User.AccountStatus status, LocalDateTime cutoff);
}
