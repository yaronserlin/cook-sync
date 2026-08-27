package com.cooksync_server.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing an unverified registration attempt awaiting email OTP confirmation.
 * Holds the submitted profile details and hashed password until the OTP is verified; only then
 * is a real {@link User} row created and this row deleted. Maps table columns in
 * "pending_registrations".
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@Entity
@Table(name = "pending_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String passwordHash;

    /**
     * Whether the user accepted the terms of use when submitting this registration. Carried
     * over verbatim to {@link User#isTermsAccepted()} once the OTP is confirmed and the real
     * account row is created.
     */
    @Column(nullable = false)
    private boolean termsAccepted;

    /**
     * Whether the user opted into marketing communications when submitting this registration.
     * Carried over verbatim to {@link User#isMarketingOptIn()} once the OTP is confirmed and the
     * real account row is created.
     */
    @Column(nullable = false)
    private boolean marketingOptIn;

    @Column(nullable = false)
    private String otpCodeHash;

    @Column(nullable = false)
    private Instant otpExpiresAt;

    @Builder.Default
    @Column(nullable = false)
    private int attemptCount = 0;
}
