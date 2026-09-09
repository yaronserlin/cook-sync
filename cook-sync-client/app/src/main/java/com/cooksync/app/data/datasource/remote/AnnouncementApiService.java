package com.cooksync.app.data.datasource.remote;

import com.dtos.request.announcement.AnnouncementCreateRequestDTO;
import com.dtos.request.common.PageRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.announcement.AnnouncementResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Retrofit contract for system announcements: the user-facing "active announcement" banner and
 * its admin-only authoring/deactivation endpoints.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface AnnouncementApiService {

    /**
     * Fetches the newest active system announcement the authenticated user hasn't dismissed
     * yet, if any.
     *
     * @return call yielding the announcement, or a null payload if none is pending
     */
    @GET("api/announcements/active")
    Call<ApiResponse<AnnouncementResponse>> getActiveAnnouncement();

    /**
     * Records that the authenticated user has dismissed ("Got it") the given announcement.
     *
     * @param id the announcement's ID
     * @return call yielding an empty acknowledgement
     */
    @POST("api/announcements/{id}/dismiss")
    Call<ApiResponse<Void>> dismissAnnouncement(@Path("id") String id);

    /**
     * Creates a new system announcement and immediately broadcasts it via push (admin-only).
     *
     * @param request the announcement content
     * @return call yielding the created announcement
     */
    @POST("api/admin/announcements")
    Call<ApiResponse<AnnouncementResponse>> createAnnouncement(@Body AnnouncementCreateRequestDTO request);

    /**
     * Fetches a paginated, newest-first list of every announcement, active or not (admin-only).
     *
     * @param params {@link PageRequestDTO#toQueryMap()} for pagination
     * @return call yielding a page of announcements
     */
    @GET("api/admin/announcements")
    Call<ApiResponse<PagedResponse<AnnouncementResponse>>> getAnnouncements(@QueryMap Map<String, String> params);

    /**
     * Deactivates an announcement so it stops being surfaced to users who haven't seen it yet
     * (admin-only).
     *
     * @param id the announcement's ID
     * @return call yielding an empty acknowledgement
     */
    @PATCH("api/admin/announcements/{id}/deactivate")
    Call<ApiResponse<Void>> deactivateAnnouncement(@Path("id") String id);
}
