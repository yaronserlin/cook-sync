package com.cooksync_server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.services.NotificationPreferencesService;
import com.dtos.request.notification.NotificationPreferencesUpdateRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.notification.NotificationPreferencesResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller managing the authenticated user's notification preferences.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@RestController
@RequestMapping("/api/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferencesService notificationPreferencesService;

    /**
     * Retrieves the authenticated user's current notification preferences.
     *
     * @param authentication active user authentication token
     * @return response entity containing the user's preferences
     */
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> getPreferences(Authentication authentication) {
        NotificationPreferencesResponse response = notificationPreferencesService.getPreferences(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Notification preferences retrieved successfully"));
    }

    /**
     * Updates the authenticated user's notification preferences.
     *
     * @param request the new preference values
     * @param authentication active user authentication token
     * @return response entity acknowledging the update
     */
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @Valid @RequestBody NotificationPreferencesUpdateRequestDTO request,
            Authentication authentication) {
        notificationPreferencesService.updatePreferences(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification preferences updated successfully"));
    }
}
