package com.cooksync_server.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled trigger that runs the daily account-deletion purge, permanently erasing every
 * account whose 30-day deletion grace period has lapsed. See {@link AccountDeletionServiceImp}
 * for the actual purge logic.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountPurgeScheduler {

    private final AccountDeletionService accountDeletionService;

    /**
     * Runs once a day and delegates to {@link AccountDeletionServiceImp#purgeExpiredAccounts()}.
     *
     * Complexity:
     * Time: O(U * P) where U is expired-account count and P is each account's data graph size
     * Space: O(P)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredAccounts() {
        log.info("Running scheduled account-deletion purge job");
        accountDeletionService.purgeExpiredAccounts();
    }
}
