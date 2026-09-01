package com.cooksync_server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test suite verifying {@link RateLimitFilter}'s per-(IP, path) fixed-window throttling:
 * requests within the limit pass through, the request that crosses it is rejected with a 429 in
 * the standard error envelope, separate client IPs get independent counters, and unguarded paths
 * are never throttled.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 01/09/2026
 */
class RateLimitFilterTest {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String UNGUARDED_PATH = "/api/auth/refresh-token";
    private static final String CLIENT_IP = "203.0.113.10";

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        // Mirrors Spring Boot's auto-configured ObjectMapper bean (what RateLimitFilter actually
        // gets injected at runtime), which auto-registers the JSR-310 module for Instant - a bare
        // `new ObjectMapper()` doesn't, and would fail serializing ApiErrorResponse#timestamp.
        filter = new RateLimitFilter(new ObjectMapper().findAndRegisterModules());
    }

    private void sendRequest(String path, String remoteAddr, MockHttpServletResponse response, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(remoteAddr);
        filter.doFilterInternal(request, response, chain);
    }

    @Test
    void doFilterInternal_ShouldAllowRequests_WhenUnderLimit() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 10; i++) {
            sendRequest(LOGIN_PATH, CLIENT_IP, response, chain);
        }

        verify(chain, times(10)).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_ShouldRejectWithTooManyRequests_WhenLimitExceeded() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 10; i++) {
            sendRequest(LOGIN_PATH, CLIENT_IP, response, chain);
        }
        response = new MockHttpServletResponse();
        sendRequest(LOGIN_PATH, CLIENT_IP, response, chain);

        verify(chain, times(10)).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertEquals("application/json", response.getContentType());

        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        assertFalse(body.get("success").asBoolean());
        assertEquals("RATE_LIMIT_EXCEEDED", body.get("error").get("errorCode").asText());
        assertEquals(429, body.get("error").get("status").asInt());
        assertEquals(LOGIN_PATH, body.get("error").get("path").asText());
    }

    @Test
    void doFilterInternal_ShouldTrackSeparateCounters_PerClientIp() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse responseA = new MockHttpServletResponse();

        for (int i = 0; i < 11; i++) {
            sendRequest(LOGIN_PATH, "198.51.100.1", responseA, chain);
        }
        assertEquals(429, responseA.getStatus());

        MockHttpServletResponse responseB = new MockHttpServletResponse();
        sendRequest(LOGIN_PATH, "198.51.100.2", responseB, chain);

        assertEquals(200, responseB.getStatus());
    }

    @Test
    void doFilterInternal_ShouldNeverThrottle_UnguardedPath() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 50; i++) {
            sendRequest(UNGUARDED_PATH, CLIENT_IP, response, chain);
        }

        verify(chain, times(50)).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }
}
