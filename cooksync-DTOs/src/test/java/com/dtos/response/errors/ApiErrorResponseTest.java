package com.dtos.response.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link ApiErrorResponse#of}, the single construction path used by every
 * error-producing call site.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class ApiErrorResponseTest {

    @Test
    void of_BuildsErrorResponse_StampedWithCurrentInstant() {
        Instant before = Instant.now();

        ApiErrorResponse error = ApiErrorResponse.of(404, "Not Found", "RESOURCE_NOT_FOUND", "Recipe not found", "/api/recipes/123");

        Instant after = Instant.now();

        assertEquals(404, error.status());
        assertEquals("Not Found", error.error());
        assertEquals("RESOURCE_NOT_FOUND", error.errorCode());
        assertEquals("Recipe not found", error.message());
        assertEquals("/api/recipes/123", error.path());
        assertNotNull(error.timestamp());
        assertTrue(!error.timestamp().isBefore(before) && !error.timestamp().isAfter(after));
    }
}
