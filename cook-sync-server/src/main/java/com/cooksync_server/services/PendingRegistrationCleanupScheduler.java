package com.cooksync_server.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled trigger that runs the daily pending-registration cleanup, permanently erasing every
 * unverified registration attempt whose OTP expired at least a day ago. See
 * {@link AuthServiceImp#purgeExpiredPendingRegistrations()} for the actual purge logic.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingRegistrationCleanupScheduler {

    private final AuthService authService;

    /**
     * Runs once a day and delegates to {@link AuthService#purgeExpiredPendingRegistrations()}.
     */
    @Scheduled(cron = "0 30 3 * * *")
    public void purgeExpiredPendingRegistrations() {
        log.info("Running scheduled pending-registration cleanup job");
        authService.purgeExpiredPendingRegistrations();
    }
}
