package com.cooksync_server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.AnnouncementDismissal;

/**
 * Spring Data JPA Repository interface for AnnouncementDismissal entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Repository
public interface AnnouncementDismissalRepository extends JpaRepository<AnnouncementDismissal, String> {

    /**
     * Checks whether a user has already dismissed a given announcement, so a repeat dismiss call
     * (e.g. a retried request) doesn't insert a duplicate row.
     *
     * @param announcementId the announcement's ID
     * @param userId the user's ID
     * @return true if a dismissal already exists
     */
    boolean existsByAnnouncementIdAndUserId(String announcementId, String userId);
}
