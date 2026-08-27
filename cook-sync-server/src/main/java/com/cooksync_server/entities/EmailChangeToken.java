package com.cooksync_server.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing a one-time, time-limited 6-digit verification code issued for a
 * pending self-service email-address change. Maps table columns in "email_change_tokens".
 * Deleted outright once consumed (successful confirmation) rather than flagged used, so a fresh
 * email-change request is always the only way to obtain a further attempt.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
@Entity
@Table(name = "email_change_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column(name = "new_email", nullable = false)
    private String newEmail;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private int attemptCount = 0;
}
