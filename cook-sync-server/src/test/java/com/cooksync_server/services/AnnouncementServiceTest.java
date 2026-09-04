package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.cooksync_server.entities.AnnouncementDismissal;
import com.cooksync_server.entities.SystemAnnouncement;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.AnnouncementDismissalRepository;
import com.cooksync_server.repositories.SystemAnnouncementRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.announcement.AnnouncementCreateRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.announcement.AnnouncementResponse;

/**
 * Unit test suite verifying system announcement authoring/broadcast, retrieval, and dismissal
 * in AnnouncementServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private SystemAnnouncementRepository announcementRepository;
    @Mock
    private AnnouncementDismissalRepository dismissalRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private AnnouncementServiceImp announcementService;

    private User admin;
    private User regularUser;

    @BeforeEach
    void setUp() {
        admin = User.builder().id("admin-1").email("admin@cooksync.com").isAdmin(true).build();
        regularUser = User.builder().id("user-1").email("gordon@cooksync.com").build();
    }

    @Test
    void create_ShouldThrowResourceNotFoundException_WhenAdminMissing() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> announcementService.create(
                new AnnouncementCreateRequestDTO("Title", "Body", "INFO"), "missing@cooksync.com"));

        verify(announcementRepository, never()).save(any());
        verify(pushNotificationService, never()).broadcast(anyString(), anyString());
    }

    @Test
    void create_ShouldSaveAnnouncementAndBroadcast() {
        when(userRepository.findByEmail("admin@cooksync.com")).thenReturn(Optional.of(admin));
        // save() is mocked, so it never actually goes through Hibernate/@PrePersist — set
        // createdAt here to mirror what persistence would have done, since toResponse() reads it.
        when(announcementRepository.save(any(SystemAnnouncement.class)))
                .thenAnswer(invocation -> {
                    SystemAnnouncement saved = invocation.getArgument(0);
                    saved.setCreatedAt(java.time.LocalDateTime.now());
                    return saved;
                });

        AnnouncementResponse response = announcementService.create(
                new AnnouncementCreateRequestDTO("Version update", "Please update", "ACTION_REQUIRED"), "admin@cooksync.com");

        ArgumentCaptor<SystemAnnouncement> captor = ArgumentCaptor.forClass(SystemAnnouncement.class);
        verify(announcementRepository).save(captor.capture());
        SystemAnnouncement saved = captor.getValue();
        assertEquals("Version update", saved.getTitle());
        assertEquals("Please update", saved.getBody());
        assertEquals(SystemAnnouncement.Severity.ACTION_REQUIRED, saved.getSeverity());
        assertEquals(admin, saved.getCreatedBy());

        verify(pushNotificationService).broadcast("Version update", "Please update");

        assertEquals("Version update", response.title());
        assertEquals("ACTION_REQUIRED", response.severity());
        assertTrue(response.active());
    }

    @Test
    void getAll_ShouldReturnPagedResponseOfAnnouncements() {
        SystemAnnouncement announcement = SystemAnnouncement.builder()
                .id("ann-1").title("Title").body("Body")
                .severity(SystemAnnouncement.Severity.INFO).active(true).createdBy(admin)
                .createdAt(java.time.LocalDateTime.now()).build();
        Page<SystemAnnouncement> page = new PageImpl<>(List.of(announcement), PageRequest.of(0, 20), 1);
        when(announcementRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        PagedResponse<AnnouncementResponse> response = announcementService.getAll(0, 20);

        assertEquals(1, response.content().size());
        assertEquals("ann-1", response.content().get(0).id());
    }

    @Test
    void deactivate_ShouldThrowResourceNotFoundException_WhenAnnouncementMissing() {
        when(announcementRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> announcementService.deactivate("missing"));
    }

    @Test
    void deactivate_ShouldSetActiveFalseAndSave() {
        SystemAnnouncement announcement = SystemAnnouncement.builder()
                .id("ann-1").active(true).severity(SystemAnnouncement.Severity.INFO).build();
        when(announcementRepository.findById("ann-1")).thenReturn(Optional.of(announcement));

        announcementService.deactivate("ann-1");

        ArgumentCaptor<SystemAnnouncement> captor = ArgumentCaptor.forClass(SystemAnnouncement.class);
        verify(announcementRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    @Test
    void getActiveForUser_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> announcementService.getActiveForUser("missing@cooksync.com"));
    }

    @Test
    void getActiveForUser_ShouldReturnEmpty_WhenNoneActive() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(regularUser));
        when(announcementRepository.findFirstActiveNotDismissedByUser("user-1")).thenReturn(Optional.empty());

        Optional<AnnouncementResponse> result = announcementService.getActiveForUser("gordon@cooksync.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveForUser_ShouldReturnMappedAnnouncement_WhenOneIsPending() {
        SystemAnnouncement announcement = SystemAnnouncement.builder()
                .id("ann-1").title("Title").body("Body")
                .severity(SystemAnnouncement.Severity.INFO).active(true)
                .createdAt(java.time.LocalDateTime.now()).build();
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(regularUser));
        when(announcementRepository.findFirstActiveNotDismissedByUser("user-1")).thenReturn(Optional.of(announcement));

        Optional<AnnouncementResponse> result = announcementService.getActiveForUser("gordon@cooksync.com");

        assertTrue(result.isPresent());
        assertEquals("ann-1", result.get().id());
    }

    @Test
    void dismiss_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> announcementService.dismiss("ann-1", "missing@cooksync.com"));
    }

    @Test
    void dismiss_ShouldThrowResourceNotFoundException_WhenAnnouncementMissing() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(regularUser));
        when(announcementRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> announcementService.dismiss("missing", "gordon@cooksync.com"));
    }

    @Test
    void dismiss_ShouldSaveDismissal_WhenNotAlreadyDismissed() {
        SystemAnnouncement announcement = SystemAnnouncement.builder().id("ann-1").build();
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(regularUser));
        when(announcementRepository.findById("ann-1")).thenReturn(Optional.of(announcement));
        when(dismissalRepository.existsByAnnouncementIdAndUserId("ann-1", "user-1")).thenReturn(false);

        announcementService.dismiss("ann-1", "gordon@cooksync.com");

        verify(dismissalRepository).save(any(AnnouncementDismissal.class));
    }

    @Test
    void dismiss_ShouldNotSaveDuplicate_WhenAlreadyDismissed() {
        SystemAnnouncement announcement = SystemAnnouncement.builder().id("ann-1").build();
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(regularUser));
        when(announcementRepository.findById("ann-1")).thenReturn(Optional.of(announcement));
        when(dismissalRepository.existsByAnnouncementIdAndUserId("ann-1", "user-1")).thenReturn(true);

        announcementService.dismiss("ann-1", "gordon@cooksync.com");

        verify(dismissalRepository, never()).save(any());
    }
}
