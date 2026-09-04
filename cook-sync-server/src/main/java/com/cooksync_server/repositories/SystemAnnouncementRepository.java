package com.cooksync_server.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.SystemAnnouncement;

/**
 * Spring Data JPA Repository interface for SystemAnnouncement entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Repository
public interface SystemAnnouncementRepository extends JpaRepository<SystemAnnouncement, String> {

    /**
     * Retrieves the newest active announcement the given user has not yet dismissed, if any.
     * "Newest" so that if an admin creates a second announcement before an earlier one is
     * deactivated, users see only the latest rather than being shown both in sequence.
     *
     * @param userId the requesting user's ID
     * @return the announcement to show the user, if any
     */
    @Query("SELECT a FROM SystemAnnouncement a WHERE a.active = true "
            + "AND a.id NOT IN (SELECT d.announcement.id FROM AnnouncementDismissal d WHERE d.user.id = :userId) "
            + "ORDER BY a.createdAt DESC")
    Optional<SystemAnnouncement> findFirstActiveNotDismissedByUser(@Param("userId") String userId);

    /**
     * Retrieves a paginated, newest-first list of every announcement (active or not), for the
     * admin management screen.
     *
     * @param pageable pagination and sorting information
     * @return page of announcement entities
     */
    Page<SystemAnnouncement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
