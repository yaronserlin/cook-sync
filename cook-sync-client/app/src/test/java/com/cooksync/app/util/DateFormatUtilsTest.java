package com.cooksync.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDate;

/**
 * Unit tests for {@link DateFormatUtils}. Covers the shared ISO-date parsing used by both
 * {@code RecipeDetailViewModel} and {@code ReviewAdapter}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
public class DateFormatUtilsTest {

    @Test
    public void parseIsoDate_parsesDatePortion_ofFullTimestamp() {
        assertEquals(LocalDate.of(2026, 4, 15), DateFormatUtils.parseIsoDate("2026-04-15T10:30:00.000Z"));
    }

    @Test
    public void parseIsoDate_parsesBareDateString() {
        assertEquals(LocalDate.of(2026, 4, 15), DateFormatUtils.parseIsoDate("2026-04-15"));
    }

    @Test
    public void parseIsoDate_null_forNullOrBlank() {
        assertNull(DateFormatUtils.parseIsoDate(null));
        assertNull(DateFormatUtils.parseIsoDate("   "));
    }

    @Test
    public void parseIsoDate_null_forUnparseableString() {
        assertNull(DateFormatUtils.parseIsoDate("not-a-date"));
    }

    @Test
    public void parseIsoDate_null_forStringShorterThanDatePortion() {
        assertNull(DateFormatUtils.parseIsoDate("2026"));
    }

    @Test
    public void formatRelativeDay_empty_forUnparseableInput() {
        assertEquals("", DateFormatUtils.formatRelativeDay("not-a-date"));
        assertEquals("", DateFormatUtils.formatRelativeDay(null));
    }

    @Test
    public void formatRelativeDay_today_forZeroDays() {
        assertEquals("Today", DateFormatUtils.formatRelativeDay(LocalDate.now().toString()));
    }

    @Test
    public void formatRelativeDay_today_forFutureDate() {
        assertEquals("Today", DateFormatUtils.formatRelativeDay(LocalDate.now().plusDays(5).toString()));
    }

    @Test
    public void formatRelativeDay_oneDayAgo() {
        assertEquals("1 day ago", DateFormatUtils.formatRelativeDay(LocalDate.now().minusDays(1).toString()));
    }

    @Test
    public void formatRelativeDay_daysAgo_atLowBoundaryOfBucket() {
        assertEquals("2 days ago", DateFormatUtils.formatRelativeDay(LocalDate.now().minusDays(2).toString()));
    }

    @Test
    public void formatRelativeDay_daysAgo_atHighBoundaryOfBucket() {
        assertEquals("29 days ago", DateFormatUtils.formatRelativeDay(LocalDate.now().minusDays(29).toString()));
    }

    @Test
    public void formatRelativeDay_oneMonthAgo_atLowBoundaryOfBucket() {
        assertEquals("1 month ago", DateFormatUtils.formatRelativeDay(LocalDate.now().minusDays(30).toString()));
    }

    @Test
    public void formatRelativeDay_monthsAgo_plural() {
        assertEquals("3 months ago", DateFormatUtils.formatRelativeDay(LocalDate.now().minusDays(90).toString()));
    }

    @Test
    public void formatRelativeDay_monthsAgo_atHighBoundaryOfBucket() {
        assertEquals("12 months ago", DateFormatUtils.formatRelativeDay(LocalDate.now().minusDays(364).toString()));
    }

    @Test
    public void formatRelativeDay_oneYearAgo_atLowBoundaryOfBucket() {
        assertEquals("1 year ago", DateFormatUtils.formatRelativeDay(LocalDate.now().minusDays(365).toString()));
    }

    @Test
    public void formatRelativeDay_yearsAgo_plural() {
        assertEquals("2 years ago", DateFormatUtils.formatRelativeDay(LocalDate.now().minusDays(800).toString()));
    }
}
