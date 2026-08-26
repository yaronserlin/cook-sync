package com.cooksync_server.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.cooksync_server.services.AdminService;
import com.dtos.request.tags.TagMergeRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.AdminStatsResponse;
import com.dtos.response.admin.DuplicateTagGroupResponse;
import com.dtos.response.admin.ReportedReviewResponse;
import com.dtos.response.user.UserResponse;
import org.springframework.web.bind.annotation.RequestParam;

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

    /**
     * Retrieves aggregated system stats for the administrative dashboard.
     *
     * @return response entity containing AdminStatsResponse payload
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(new ApiResponse<>(true, adminService.getStats(), null, "Stats retrieved successfully"));
    }

    /**
     * Retrieves paginated list of registered user accounts, optionally search-filtered by
     * name/email, filtered by enabled status, and sorted.
     *
     * @param page zero-based page index
     * @param size page size limit
     * @param q optional search fragment matched against first name, last name, or email
     * @param enabled optional account status filter (true = active, false = disabled)
     * @param sortBy field to sort by: firstName, lastName, email, or createdAt (default createdAt)
     * @param direction sort direction, "asc" or "desc" (default desc)
     * @return response entity containing PagedResponse of UserResponse DTOs
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(new ApiResponse<>(true, adminService.getAllUsers(page, size, q, enabled, sortBy, direction), null, "Users retrieved successfully"));
    }

    /**
     * Retrieves list of review entries flagged as reported.
     *
     * @return response entity containing list of ReportedReviewResponse DTOs
     */
    @GetMapping("/reviews/reported")
    public ResponseEntity<ApiResponse<PagedResponse<ReportedReviewResponse>>> getReportedReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, adminService.getReportedReviews(page, size), null, "Reported reviews retrieved successfully"));
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
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Report dismissed"));
    }

    /**
     * Suspends user account with specified ID.
     *
     * @param id target user ID
     * @return response entity acknowledging account suspension
     */
    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable String id) {
        adminService.suspendUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "User suspended"));
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
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "User enabled"));
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
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "User permanently deleted"));
    }

    /**
     * Detects and groups potential duplicate tags for consolidation audit.
     *
     * @return response entity containing list of DuplicateTagGroupResponse DTOs
     */
    @GetMapping("/tags/duplicates")
    public ResponseEntity<ApiResponse<PagedResponse<DuplicateTagGroupResponse>>> getDuplicateTagGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, adminService.getDuplicateTagGroups(page, size), null, "Duplicate tag groups retrieved successfully"));
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
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Tags merged successfully"));
    }
}
