package com.cooksync_server.entities;

import com.cooksync_server.constants.SchemaConstants;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity recording that a given user has dismissed ("Got it") a given
 * {@link SystemAnnouncement}, so it is not shown to them again. Maps table columns in
 * "announcement_dismissals".
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Entity
@Table(name = "announcement_dismissals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"announcement_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementDismissal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private SystemAnnouncement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "dismissed_at", nullable = false, updatable = false)
    private LocalDateTime dismissedAt;

    /**
     * Initializes dismissal timestamp before entity persistence.
     */
    @PrePersist
    protected void onCreate() {
        dismissedAt = LocalDateTime.now();
    }
}
