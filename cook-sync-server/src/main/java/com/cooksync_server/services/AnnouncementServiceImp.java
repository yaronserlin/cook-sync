package com.cooksync_server.services;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.constants.EntityNames;
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

import lombok.RequiredArgsConstructor;

/**
 * Service class managing system announcement authoring, broadcast, retrieval, and dismissal.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImp implements AnnouncementService {

    private final SystemAnnouncementRepository announcementRepository;
    private final AnnouncementDismissalRepository dismissalRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    @Override
    @Transactional
    public AnnouncementResponse create(AnnouncementCreateRequestDTO request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, adminEmail));

        SystemAnnouncement announcement = SystemAnnouncement.builder()
                .title(request.title())
                .body(request.body())
                .severity(SystemAnnouncement.Severity.valueOf(request.severity()))
                .createdBy(admin)
                .build();
        announcement = announcementRepository.save(announcement);

        pushNotificationService.broadcast(announcement.getTitle(), announcement.getBody());

        return toResponse(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AnnouncementResponse> getAll(int page, int size) {
        Page<SystemAnnouncement> announcements = announcementRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return PagedResponseMapper.toPagedResponse(announcements, this::toResponse);
    }

    @Override
    @Transactional
    public void deactivate(String id) {
        SystemAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
        announcement.setActive(false);
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnnouncementResponse> getActiveForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));
        return announcementRepository.findFirstActiveNotDismissedByUser(user.getId()).map(this::toResponse);
    }

    @Override
    @Transactional
    public void dismiss(String announcementId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));
        SystemAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", announcementId));

        if (dismissalRepository.existsByAnnouncementIdAndUserId(announcementId, user.getId())) {
            return;
        }
        dismissalRepository.save(AnnouncementDismissal.builder()
                .announcement(announcement)
                .user(user)
                .build());
    }

    private AnnouncementResponse toResponse(SystemAnnouncement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getSeverity().name(),
                announcement.isActive(),
                announcement.getCreatedAt().toString());
    }
}
