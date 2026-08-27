package com.cooksync_server.services;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test suite verifying the scheduled cleanup trigger delegates to AuthService.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
@ExtendWith(MockitoExtension.class)
class PendingRegistrationCleanupSchedulerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private PendingRegistrationCleanupScheduler pendingRegistrationCleanupScheduler;

    @Test
    void purgeExpiredPendingRegistrations_ShouldDelegateToAuthService() {
        pendingRegistrationCleanupScheduler.purgeExpiredPendingRegistrations();

        verify(authService, times(1)).purgeExpiredPendingRegistrations();
    }
}
