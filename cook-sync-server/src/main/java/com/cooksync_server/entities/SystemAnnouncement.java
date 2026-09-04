package com.cooksync_server.entities;

import com.cooksync_server.constants.SchemaConstants;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing one admin-authored system announcement broadcast to every user (e.g. a
 * mandatory app-update notice or a privacy-policy change). Maps table columns in
 * "system_announcements".
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Entity
@Table(name = "system_announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Initializes creation timestamp before entity persistence.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Enum representing how forcefully an announcement should be surfaced. {@code INFO} is a
     * routine notice a user can permanently silence via notification preferences;
     * {@code ACTION_REQUIRED} (e.g. a mandatory app update) is not silenceable there.
     */
    public enum Severity {
        INFO, ACTION_REQUIRED
    }
}
