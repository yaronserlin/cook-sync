package com.cooksync_server.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.entities.NotificationPreferences;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.NotificationPreferencesRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.notification.NotificationPreferencesUpdateRequestDTO;
import com.dtos.response.notification.NotificationPreferencesResponse;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing the authenticated user's notification preferences.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Service
@RequiredArgsConstructor
public class NotificationPreferencesServiceImp implements NotificationPreferencesService {

    private final NotificationPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public NotificationPreferencesResponse getPreferences(String userEmail) {
        NotificationPreferences preferences = findOrCreate(userEmail);
        return toResponse(preferences);
    }

    @Override
    @Transactional
    public void updatePreferences(String userEmail, NotificationPreferencesUpdateRequestDTO request) {
        NotificationPreferences preferences = findOrCreate(userEmail);
        preferences.setSystemAnnouncements(request.systemAnnouncements());
        preferences.setPushEnabled(request.pushEnabled());
        preferencesRepository.save(preferences);
    }

    /**
     * Retrieves the given user's preferences row, creating it with the all-enabled defaults on
     * first access — every user is expected to eventually have exactly one row, but none is
     * created at registration time, so this covers a user who hasn't touched their preferences
     * yet.
     *
     * @param userEmail authenticated user email address
     * @return the user's existing or newly-created preferences row
     * @throws ResourceNotFoundException if no user with the given email exists
     */
    private NotificationPreferences findOrCreate(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));

        return preferencesRepository.findById(user.getId())
                .orElseGet(() -> preferencesRepository.save(
                        NotificationPreferences.builder().user(user).build()));
    }

    private NotificationPreferencesResponse toResponse(NotificationPreferences preferences) {
        return new NotificationPreferencesResponse(preferences.isSystemAnnouncements(), preferences.isPushEnabled());
    }
}
