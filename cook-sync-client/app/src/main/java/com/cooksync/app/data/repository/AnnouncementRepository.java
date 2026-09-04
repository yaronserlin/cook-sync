package com.cooksync.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.domain.ApiResult;
import com.dtos.response.announcement.AnnouncementResponse;

/**
 * Interface contract for the authenticated user's view of system announcements.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface AnnouncementRepository {

    /**
     * Fetches the newest active system announcement the authenticated user hasn't dismissed
     * yet, if any. The server returns a null payload (not an error) when none is pending.
     *
     * @param resultTarget LiveData target to post the outcome
     */
    void getActiveAnnouncement(MutableLiveData<ApiResult<AnnouncementResponse>> resultTarget);

    /**
     * Records that the authenticated user has dismissed ("Got it") the given announcement.
     *
     * @param announcementId the announcement's ID
     * @param resultTarget LiveData target to post the outcome
     */
    void dismiss(String announcementId, MutableLiveData<ApiResult<Void>> resultTarget);
}
