package com.cooksync_server.entities;

import com.cooksync_server.constants.SchemaConstants;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity mapping the {@code users} table: represents a registered CookSync account,
 * including its credentials, profile details, privacy preferences, and account-status/
 * deletion lifecycle state.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;

    @Builder.Default
    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Enumerates the reasons an account can be non-enabled, tracked separately from
     * {@link #enabled} so the admin console can distinguish a user who deactivated their own
     * account from one an admin suspended, while {@link #enabled} itself continues to drive
     * authentication and recipe-visibility checks unchanged.
     */
    public enum AccountStatus {
        ACTIVE,
        DEACTIVATED,
        SUSPENDED
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "avatar_url", length = 2000)
    private String avatarUrl;

    /**
     * Whether the user accepted the terms of use at registration. Write-only by design: this is
     * a consent record kept for legal/audit purposes, not a value any server or client logic
     * reads back afterward.
     */
    @Builder.Default
    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted = false;

    /**
     * Whether the user opted into marketing communications at registration. Write-only by
     * design, as with {@link #termsAccepted}: retained purely as a consent record, never read
     * back by any server or client logic.
     */
    @Builder.Default
    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn = false;

    @Column(length = 255)
    private String city;

    @Column(length = 1000)
    private String bio;

    @Builder.Default
    @Column(name = "show_recipes_publicly", nullable = false)
    private boolean showRecipesPublicly = true;

    @Builder.Default
    @Column(name = "show_favorites_publicly", nullable = false)
    private boolean showFavoritesPublicly = false;

    /**
     * Timestamp of a self-service account-deletion request, distinct from a plain
     * {@link AccountStatus#DEACTIVATED} deactivation. {@code null} means the account was never
     * asked to be deleted (or a prior request was cancelled by logging back in). When set, it
     * anchors the 30-day grace period after which the scheduled purge job permanently erases the
     * account.
     */
    @Column(name = "deletion_requested_at")
    private LocalDateTime deletionRequestedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Populates the creation and last-modified timestamps immediately before the entity is
     * first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Builds the user's trimmed full name from first and last name, with a single space
     * separator and no leading/trailing whitespace.
     *
     * @return the formatted full name
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            fullName.append(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }
        return fullName.toString();
    }

    /**
     * Refreshes the last-modified timestamp immediately before an update is persisted.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
