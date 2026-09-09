package com.dtos.request.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link PaginationDefaults}'s null-to-default normalization, shared by
 * every paginated request DTO's compact constructor.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class PaginationDefaultsTest {

    @Test
    void normalizePage_ReturnsDefault_WhenNull() {
        assertEquals(PaginationDefaults.DEFAULT_PAGE, PaginationDefaults.normalizePage(null));
    }

    @Test
    void normalizePage_ReturnsGivenValue_WhenNonNull() {
        assertEquals(3, PaginationDefaults.normalizePage(3));
    }

    @Test
    void normalizeSize_ReturnsDefault_WhenNull() {
        assertEquals(PaginationDefaults.DEFAULT_SIZE, PaginationDefaults.normalizeSize(null));
    }

    @Test
    void normalizeSize_ReturnsGivenValue_WhenNonNull() {
        assertEquals(50, PaginationDefaults.normalizeSize(50));
    }
}
