package com.cooksync_server.controllers;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.services.AnnouncementService;
import com.dtos.response.ApiResponse;
import com.dtos.response.announcement.AnnouncementResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller exposing the authenticated user's view of system announcements: the current
 * one to show, and dismissing it. Announcement authoring/management is an admin-only operation,
 * exposed instead on {@link AdminController}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * Retrieves the newest active announcement the authenticated user hasn't dismissed yet, if
     * any — checked on every app launch as a fallback for users who missed the push notification.
     *
     * @param authentication active user authentication token
     * @return response entity containing the announcement, or a null payload if none is pending
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getActive(Authentication authentication) {
        Optional<AnnouncementResponse> announcement = announcementService.getActiveForUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(announcement.orElse(null), "Active announcement retrieved"));
    }

    /**
     * Records that the authenticated user has dismissed ("Got it") the given announcement, so it
     * is not shown to them again.
     *
     * @param id the announcement's ID
     * @param authentication active user authentication token
     * @return response entity acknowledging the dismissal
     */
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable String id, Authentication authentication) {
        announcementService.dismiss(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Announcement dismissed"));
    }
}
