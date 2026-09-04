package com.cooksync_server.services;

import com.dtos.request.notification.NotificationPreferencesUpdateRequestDTO;
import com.dtos.response.notification.NotificationPreferencesResponse;

/**
 * Service managing the authenticated user's notification preferences.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public interface NotificationPreferencesService {

    /**
     * Retrieves the given user's notification preferences, creating the default row (everything
     * enabled) on first access if none exists yet.
     *
     * @param userEmail authenticated user email address
     * @return the user's current notification preferences
     */
    NotificationPreferencesResponse getPreferences(String userEmail);

    /**
     * Replaces the given user's notification preferences.
     *
     * @param userEmail authenticated user email address
     * @param request the new preference values
     */
    void updatePreferences(String userEmail, NotificationPreferencesUpdateRequestDTO request);
}
