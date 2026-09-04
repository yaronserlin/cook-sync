package com.cooksync_server.services;

import java.util.Optional;

import com.dtos.request.announcement.AnnouncementCreateRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.announcement.AnnouncementResponse;

/**
 * Service managing system announcements: admin authoring/broadcast, and per-user
 * retrieval/dismissal.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface AnnouncementService {

    /**
     * Creates a new system announcement and immediately broadcasts it via push to every
     * push-enabled device.
     *
     * @param request the announcement content
     * @param adminEmail the authoring admin's email address
     * @return the created announcement
     */
    AnnouncementResponse create(AnnouncementCreateRequestDTO request, String adminEmail);

    /**
     * Retrieves a paginated, newest-first list of every announcement, for the admin management
     * screen.
     *
     * @param page zero-based page index
     * @param size page size limit
     * @return page of AnnouncementResponse DTOs
     */
    PagedResponse<AnnouncementResponse> getAll(int page, int size);

    /**
     * Deactivates an announcement so it stops being surfaced to users who haven't seen it yet.
     * Does not affect users who already dismissed it.
     *
     * @param id the announcement's ID
     */
    void deactivate(String id);

    /**
     * Retrieves the newest active announcement the given user hasn't dismissed yet, if any.
     *
     * @param userEmail authenticated user email address
     * @return the announcement to show, if any
     */
    Optional<AnnouncementResponse> getActiveForUser(String userEmail);

    /**
     * Records that the given user has dismissed the given announcement, so it is not shown to
     * them again.
     *
     * @param announcementId the announcement's ID
     * @param userEmail authenticated user email address
     */
    void dismiss(String announcementId, String userEmail);
}
