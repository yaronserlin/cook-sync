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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing one installed device's push-notification registration. Deliberately
 * not one-row-per-user (unlike {@link RefreshToken}, which enforces exactly one active session
 * per account): a user may have the app installed on several devices at once, each holding its
 * own push token, so {@code pushToken} rather than {@code user} carries the uniqueness
 * constraint. Maps table columns in "device_tokens".
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "push_token", nullable = false, unique = true, length = 255)
    private String pushToken;

    @Column(name = "platform", nullable = false, length = 16)
    private String platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    /**
     * Initializes creation/last-seen timestamps before entity persistence.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastSeenAt = createdAt;
    }

    /**
     * Refreshes the last-seen timestamp before an update (e.g. re-registering an already-known
     * token on a later app launch).
     */
    @PreUpdate
    protected void onUpdate() {
        lastSeenAt = LocalDateTime.now();
    }
}
