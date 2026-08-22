package com.cooksync_server.services;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test suite verifying the scheduled purge trigger delegates to AccountDeletionServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class AccountPurgeSchedulerTest {

    @Mock
    private AccountDeletionService accountDeletionService;

    @InjectMocks
    private AccountPurgeScheduler accountPurgeScheduler;

    @Test
    void purgeExpiredAccounts_ShouldDelegateToAccountDeletionService() {
        accountPurgeScheduler.purgeExpiredAccounts();

        verify(accountDeletionService, times(1)).purgeExpiredAccounts();
    }
}
