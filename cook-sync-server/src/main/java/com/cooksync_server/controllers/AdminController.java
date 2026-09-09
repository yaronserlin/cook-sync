package com.cooksync_server.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.cooksync_server.services.AdminService;
import com.cooksync_server.services.AnnouncementService;
import com.cooksync_server.services.AppConfigService;
import com.dtos.request.admin.AdminUserQueryRequestDTO;
import com.dtos.request.announcement.AnnouncementCreateRequestDTO;
import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.appconfig.AppConfigUpdateRequestDTO;
import com.dtos.request.tags.TagMergeRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.AdminStatsResponse;
import com.dtos.response.admin.DuplicateTagGroupResponse;
import com.dtos.response.admin.ReportedReviewResponse;
import com.dtos.response.announcement.AnnouncementResponse;
import com.dtos.response.appconfig.AppConfigResponse;
import com.dtos.response.user.UserResponse;
import org.springframework.web.bind.annotation.PutMapping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller exposing administrative operations for system moderation, user management, and tag deduplication.
 * Protected by administrative role authorization.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AnnouncementService announcementService;
    private final AppConfigService appConfigService;

    /**
     * Retrieves aggregated system stats for the administrative dashboard.
     *
     * @return response entity containing AdminStatsResponse payload
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getStats(), "Stats retrieved successfully"));
    }

    /**
     * Retrieves paginated list of registered user accounts, optionally search-filtered by
     * name/email, filtered by enabled status, and sorted.
     *
     * @param request the directory's pagination, search, filter, and sort parameters, bound from
     *                query parameters
     * @return response entity containing PagedResponse of UserResponse DTOs
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(@Valid @ModelAttribute AdminUserQueryRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers(request), "Users retrieved successfully"));
    }

    /**
     * Retrieves list of review entries flagged as reported.
     *
     * @param page zero-based page index
     * @param size page size limit
     * @return response entity containing list of ReportedReviewResponse DTOs
     */
    @GetMapping("/reviews/reported")
    public ResponseEntity<ApiResponse<PagedResponse<ReportedReviewResponse>>> getReportedReviews(@ModelAttribute PageRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getReportedReviews(request), "Reported reviews retrieved successfully"));
    }

    /**
     * Dismisses moderation report for specified review ID.
     *
     * @param id target review ID
     * @return response entity acknowledging report dismissal
     */
    @PostMapping("/reviews/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismissReport(@PathVariable String id) {
        adminService.dismissReport(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Report dismissed"));
    }

    /**
     * Suspends user account with specified ID. Refuses to suspend the acting admin's own
     * account or any other admin account.
     *
     * @param id target user ID
     * @param authentication the acting admin's authentication, used for the self-suspension guard
     * @return response entity acknowledging account suspension
     */
    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable String id, Authentication authentication) {
        adminService.suspendUser(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "User suspended"));
    }

    /**
     * Enables user account with specified ID.
     *
     * @param id target user ID
     * @return response entity acknowledging account enabling
     */
    @PatchMapping("/users/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable String id) {
        adminService.enableUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User enabled"));
    }

    /**
     * Permanently deletes a user account and everything it owns, bypassing the normal 30-day
     * self-service deletion grace period. Refuses to delete the acting admin's own account or
     * any other admin account.
     *
     * @param id target user ID
     * @param authentication the acting admin's authentication, used for the self-deletion guard
     * @return response entity acknowledging permanent deletion
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id, Authentication authentication) {
        adminService.deleteUserPermanently(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "User permanently deleted"));
    }

    /**
     * Detects and groups potential duplicate tags for consolidation audit.
     *
     * @param request pagination parameters
     * @return response entity containing list of DuplicateTagGroupResponse DTOs
     */
    @GetMapping("/tags/duplicates")
    public ResponseEntity<ApiResponse<PagedResponse<DuplicateTagGroupResponse>>> getDuplicateTagGroups(@ModelAttribute PageRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDuplicateTagGroups(request), "Duplicate tag groups retrieved successfully"));
    }

    /**
     * Merges source duplicate tag into canonical target tag and deletes source.
     *
     * @param request tag merge payload containing source and target tag IDs
     * @return response entity acknowledging tag merge completion
     */
    @PostMapping("/tags/merge")
    public ResponseEntity<ApiResponse<Void>> mergeTags(@Valid @RequestBody TagMergeRequestDTO request) {
        adminService.mergeTags(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Tags merged successfully"));
    }

    /**
     * Creates a new system announcement and immediately broadcasts it via push to every
     * push-enabled device.
     *
     * @param request the announcement content
     * @param authentication the authoring admin's authentication
     * @return response entity containing the created AnnouncementResponse DTO
     */
    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody AnnouncementCreateRequestDTO request,
            Authentication authentication) {
        AnnouncementResponse response = announcementService.create(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement created and broadcast"));
    }

    /**
     * Retrieves a paginated, newest-first list of every announcement, active or not.
     *
     * @param request pagination parameters
     * @return response entity containing PagedResponse of AnnouncementResponse DTOs
     */
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> getAnnouncements(@ModelAttribute PageRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.getAll(request), "Announcements retrieved successfully"));
    }

    /**
     * Deactivates an announcement so it stops being surfaced to users who haven't seen it yet.
     *
     * @param id the announcement's ID
     * @return response entity acknowledging deactivation
     */
    @PatchMapping("/announcements/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAnnouncement(@PathVariable String id) {
        announcementService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Announcement deactivated"));
    }

    /**
     * Updates a platform's minimum supported client version and download link, backing the
     * client's forced-update gate.
     *
     * @param request the new configuration
     * @param authentication the acting admin's authentication
     * @return response entity containing the saved AppConfigResponse DTO
     */
    @PutMapping("/app-config")
    public ResponseEntity<ApiResponse<AppConfigResponse>> updateAppConfig(
            @Valid @RequestBody AppConfigUpdateRequestDTO request,
            Authentication authentication) {
        AppConfigResponse response = appConfigService.updateConfig(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "App config updated successfully"));
    }
}
