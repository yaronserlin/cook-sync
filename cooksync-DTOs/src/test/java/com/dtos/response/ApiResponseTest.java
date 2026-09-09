package com.dtos.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link ApiResponse}'s {@link ApiResponse#success} and
 * {@link ApiResponse#error} factory methods.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class ApiResponseTest {

    @Test
    void success_BuildsSuccessfulResponse_WithDataAndMessage() {
        ApiResponse<String> response = ApiResponse.success("payload", "It worked");

        assertTrue(response.success());
        assertEquals("payload", response.data());
        assertNull(response.error());
        assertEquals("It worked", response.message());
    }

    @Test
    void error_BuildsFailedResponse_WithErrorAndMessage() {
        ApiResponse<Void> response = ApiResponse.error("boom", "It failed");

        assertFalse(response.success());
        assertNull(response.data());
        assertEquals("boom", response.error());
        assertEquals("It failed", response.message());
    }
}
