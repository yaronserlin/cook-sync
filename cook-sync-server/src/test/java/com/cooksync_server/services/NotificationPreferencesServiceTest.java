package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.NotificationPreferences;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.NotificationPreferencesRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.notification.NotificationPreferencesUpdateRequestDTO;
import com.dtos.response.notification.NotificationPreferencesResponse;

/**
 * Unit test suite verifying notification-preferences retrieval (including default-row creation
 * on first access) and updates in NotificationPreferencesServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferencesServiceTest {

    @Mock
    private NotificationPreferencesRepository preferencesRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPreferencesServiceImp notificationPreferencesService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-1").email("gordon@cooksync.com").build();
    }

    @Test
    void getPreferences_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationPreferencesService.getPreferences("missing@cooksync.com"));
    }

    @Test
    void getPreferences_ShouldReturnExistingRow_WhenOneAlreadyExists() {
        NotificationPreferences existing = NotificationPreferences.builder()
                .userId("user-1").user(user).systemAnnouncements(false).pushEnabled(true).build();
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(user));
        when(preferencesRepository.findById("user-1")).thenReturn(Optional.of(existing));

        NotificationPreferencesResponse response = notificationPreferencesService.getPreferences("gordon@cooksync.com");

        assertFalse(response.systemAnnouncements());
        assertTrue(response.pushEnabled());
        verify(preferencesRepository, never()).save(any());
    }

    @Test
    void getPreferences_ShouldCreateAllEnabledDefaults_WhenNoRowExistsYet() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(user));
        when(preferencesRepository.findById("user-1")).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(NotificationPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferencesResponse response = notificationPreferencesService.getPreferences("gordon@cooksync.com");

        assertTrue(response.systemAnnouncements());
        assertTrue(response.pushEnabled());
        verify(preferencesRepository).save(any(NotificationPreferences.class));
    }

    @Test
    void updatePreferences_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationPreferencesService.updatePreferences(
                "missing@cooksync.com", new NotificationPreferencesUpdateRequestDTO(false, false)));
    }

    @Test
    void updatePreferences_ShouldSaveNewValues_OnExistingRow() {
        NotificationPreferences existing = NotificationPreferences.builder()
                .userId("user-1").user(user).systemAnnouncements(true).pushEnabled(true).build();
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(user));
        when(preferencesRepository.findById("user-1")).thenReturn(Optional.of(existing));

        notificationPreferencesService.updatePreferences("gordon@cooksync.com",
                new NotificationPreferencesUpdateRequestDTO(false, true));

        ArgumentCaptor<NotificationPreferences> captor = ArgumentCaptor.forClass(NotificationPreferences.class);
        verify(preferencesRepository).save(captor.capture());
        assertFalse(captor.getValue().isSystemAnnouncements());
        assertTrue(captor.getValue().isPushEnabled());
    }

    @Test
    void updatePreferences_ShouldCreateThenSave_WhenNoRowExistsYet() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(user));
        when(preferencesRepository.findById("user-1")).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(NotificationPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationPreferencesService.updatePreferences("gordon@cooksync.com",
                new NotificationPreferencesUpdateRequestDTO(false, false));

        // Once from findOrCreate's default-row creation, once to persist the requested values.
        verify(preferencesRepository, org.mockito.Mockito.times(2)).save(any(NotificationPreferences.class));
    }
}
