package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

/**
 * Verifies the cron expressions on the scheduled purge jobs actually resolve to the intended
 * daily fire time, independent of the Mockito-based delegation tests for each scheduler. Reads
 * the {@code cron} attribute via reflection off the live {@link Scheduled} annotation so a typo
 * or accidental removal of the annotation fails this test rather than silently going unnoticed.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
class CronScheduleExpressionsTest {

    @Test
    void accountPurgeScheduler_ShouldFireDailyAt3AM() throws NoSuchMethodException {
        assertFiresDailyAt(AccountPurgeScheduler.class, "purgeExpiredAccounts", LocalTime.of(3, 0));
    }

    @Test
    void pendingRegistrationCleanupScheduler_ShouldFireDailyAt330AM() throws NoSuchMethodException {
        assertFiresDailyAt(PendingRegistrationCleanupScheduler.class, "purgeExpiredPendingRegistrations", LocalTime.of(3, 30));
    }

    private void assertFiresDailyAt(Class<?> schedulerClass, String methodName, LocalTime expectedTime) throws NoSuchMethodException {
        Method method = schedulerClass.getDeclaredMethod(methodName);
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertNotNull(scheduled, methodName + " must remain annotated with @Scheduled");

        CronExpression cron = CronExpression.parse(scheduled.cron());
        LocalDateTime midnight = LocalDate.now().atStartOfDay();
        LocalDateTime firstFire = cron.next(midnight);
        LocalDateTime secondFire = cron.next(firstFire);

        assertEquals(expectedTime, firstFire.toLocalTime());
        assertEquals(firstFire.plusDays(1), secondFire, "job should recur every 24h, not e.g. weekly/monthly");
    }
}
