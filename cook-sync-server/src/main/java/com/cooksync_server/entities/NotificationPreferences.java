package com.cooksync_server.entities;

import com.cooksync_server.constants.SchemaConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity holding one user's notification preferences, keyed directly by that user's ID
 * (unlike most entities in this codebase, there is no separate generated UUID: exactly one row
 * ever exists per user, so the user's own ID is the natural primary key). Maps table columns in
 * "notification_preferences". Expected to grow additional per-category boolean columns as later
 * roadmap phases (e.g. group activity) introduce their own notification types.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @Column(name = "user_id", length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "system_announcements", nullable = false)
    private boolean systemAnnouncements = true;

    @Builder.Default
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;
}
