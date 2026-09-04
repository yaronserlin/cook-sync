package com.cooksync.app.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.repository.AdminRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.base.BaseViewModel;
import com.dtos.response.PagedResponse;
import com.dtos.response.announcement.AnnouncementResponse;

/**
 * Manages the Admin Console's Announcements tab: composing/broadcasting a new system
 * announcement, listing every past announcement, and deactivating one.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class AdminAnnouncementsViewModel extends BaseViewModel {

    private final AdminRepository adminRepository;

    private final MutableLiveData<ApiResult<PagedResponse<AnnouncementResponse>>> announcementsResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<AnnouncementResponse>> createResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<Void>> deactivateResult = new MutableLiveData<>();

    /**
     * Constructs the ViewModel with the given repository, injected by
     * {@link com.cooksync.app.ui.base.ViewModelFactory}.
     *
     * @param adminRepository the repository used for the Announcements tab's calls
     */
    public AdminAnnouncementsViewModel(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    /** @return observable list of every announcement (Loading → Success/Error) */
    public LiveData<ApiResult<PagedResponse<AnnouncementResponse>>> getAnnouncementsResult() { return announcementsResult; }
    /** @return observable result of the most recent announcement-creation call */
    public LiveData<ApiResult<AnnouncementResponse>> getCreateResult() { return createResult; }
    /** @return observable result of the most recent announcement-deactivation call */
    public LiveData<ApiResult<Void>> getDeactivateResult() { return deactivateResult; }

    /**
     * Loads the first page of every announcement, newest first. The Announcements tab shows
     * this as a flat, non-paginated list (an admin authoring occasional broadcasts is not
     * expected to accumulate enough of them to need scrolling pagination), so this always
     * re-fetches page 0 rather than appending further pages.
     */
    public void loadAnnouncements() {
        adminRepository.getAnnouncements(0, 50, announcementsResult);
    }

    /**
     * Creates and immediately broadcasts a new system announcement.
     *
     * @param title the announcement's short headline
     * @param body the announcement's full message body
     * @param severity "INFO" or "ACTION_REQUIRED"
     */
    public void createAnnouncement(String title, String body, String severity) {
        adminRepository.createAnnouncement(title, body, severity, createResult);
    }

    /**
     * Deactivates an announcement so it stops being surfaced to users who haven't seen it yet.
     *
     * @param id the announcement's ID
     */
    public void deactivateAnnouncement(String id) {
        adminRepository.deactivateAnnouncement(id, deactivateResult);
    }
}
