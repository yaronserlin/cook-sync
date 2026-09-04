package com.cooksync_server.entities;

import com.cooksync_server.constants.SchemaConstants;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
 * JPA Entity holding one platform's minimum supported client version and download link — one row
 * per platform (currently just {@code "ANDROID"}), the platform name itself being the natural
 * primary key since there is exactly one row per platform, matching {@link NotificationPreferences}'s
 * natural-key pattern. Maps table columns in "app_config".
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Entity
@Table(name = "app_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersionConfig {

    @Id
    @Column(name = "platform", length = 16)
    private String platform;

    @Column(name = "min_supported_version_code", nullable = false)
    private int minSupportedVersionCode;

    @Column(name = "download_url", length = 2048)
    private String downloadUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Initializes the modification timestamp before entity persistence (relevant only if a row
     * for a new platform is ever created through JPA — the existing "ANDROID" row is seeded
     * directly by the migration).
     */
    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Refreshes modification timestamp prior to update execution.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
