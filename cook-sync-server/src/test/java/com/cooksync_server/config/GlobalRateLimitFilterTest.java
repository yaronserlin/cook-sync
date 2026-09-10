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
 * Unit test suite verifying {@link GlobalRateLimitFilter}'s blanket per-IP fixed-window
 * throttling: requests within the limit pass through, the request that crosses it is rejected
 * with a 429 in the standard error envelope, separate client IPs get independent counters,
 * exempt infra paths are never throttled, and the counter resets once its window elapses.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
class GlobalRateLimitFilterTest {

    private static final String RECIPES_PATH = "/api/recipes";
    private static final String CLIENT_IP = "203.0.113.10";
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MILLIS = 60_000;

    private GlobalRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        // Mirrors Spring Boot's auto-configured ObjectMapper bean (what GlobalRateLimitFilter
        // actually gets injected at runtime), which auto-registers the JSR-310 module for
        // Instant - a bare `new ObjectMapper()` doesn't, and would fail serializing
        // ApiErrorResponse#timestamp.
        filter = new GlobalRateLimitFilter(new ObjectMapper().findAndRegisterModules(), MAX_REQUESTS, WINDOW_MILLIS);
    }

    private void sendRequest(String path, String remoteAddr, MockHttpServletResponse response, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(remoteAddr);
        filter.doFilterInternal(request, response, chain);
    }

    private void sendThroughFilterChain(String path, String remoteAddr, MockHttpServletResponse response, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(remoteAddr);
        filter.doFilter(request, response, chain);
    }

    @Test
    void doFilterInternal_ShouldAllowRequests_WhenUnderLimit() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < MAX_REQUESTS; i++) {
            sendRequest(RECIPES_PATH, CLIENT_IP, response, chain);
        }

        verify(chain, times(MAX_REQUESTS)).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_ShouldRejectWithTooManyRequests_WhenLimitExceeded() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < MAX_REQUESTS; i++) {
            sendRequest(RECIPES_PATH, CLIENT_IP, response, chain);
        }
        response = new MockHttpServletResponse();
        sendRequest(RECIPES_PATH, CLIENT_IP, response, chain);

        verify(chain, times(MAX_REQUESTS)).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertEquals("application/json", response.getContentType());

        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        assertFalse(body.get("success").asBoolean());
        assertEquals("RATE_LIMIT_EXCEEDED", body.get("error").get("errorCode").asText());
        assertEquals(429, body.get("error").get("status").asInt());
        assertEquals(RECIPES_PATH, body.get("error").get("path").asText());
    }

    @Test
    void doFilterInternal_ShouldTrackSeparateCounters_PerClientIp() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse responseA = new MockHttpServletResponse();

        for (int i = 0; i < MAX_REQUESTS + 1; i++) {
            sendRequest(RECIPES_PATH, "198.51.100.1", responseA, chain);
        }
        assertEquals(429, responseA.getStatus());

        MockHttpServletResponse responseB = new MockHttpServletResponse();
        sendRequest(RECIPES_PATH, "198.51.100.2", responseB, chain);

        assertEquals(200, responseB.getStatus());
    }

    @Test
    void doFilterInternal_ShouldTrackCounterAcrossDifferentPaths_SameIp() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < MAX_REQUESTS; i++) {
            sendRequest("/api/recipes/" + i, CLIENT_IP, response, chain);
        }
        response = new MockHttpServletResponse();
        sendRequest("/api/tags", CLIENT_IP, response, chain);

        assertEquals(429, response.getStatus());
    }

    @Test
    void shouldNotFilter_ShouldExemptHealthCheckPath() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < MAX_REQUESTS + 5; i++) {
            sendThroughFilterChain("/actuator/health", CLIENT_IP, response, chain);
        }

        verify(chain, times(MAX_REQUESTS + 5)).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldNotFilter_ShouldExemptAppConfigPath() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < MAX_REQUESTS + 5; i++) {
            sendThroughFilterChain("/api/app-config", CLIENT_IP, response, chain);
        }

        verify(chain, times(MAX_REQUESTS + 5)).doFilter(any(), any());
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_ShouldResetCounter_AfterWindowElapses() throws Exception {
        // A wide enough window that two back-to-back in-process calls reliably land inside it
        // (avoiding flakiness from GC pauses/JIT warm-up), while the sleep below reliably clears it.
        GlobalRateLimitFilter shortWindowFilter = new GlobalRateLimitFilter(new ObjectMapper().findAndRegisterModules(), 1, 300);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest first = new MockHttpServletRequest("GET", RECIPES_PATH);
        first.setRemoteAddr(CLIENT_IP);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        shortWindowFilter.doFilterInternal(first, firstResponse, chain);
        assertEquals(200, firstResponse.getStatus());

        MockHttpServletRequest second = new MockHttpServletRequest("GET", RECIPES_PATH);
        second.setRemoteAddr(CLIENT_IP);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        shortWindowFilter.doFilterInternal(second, secondResponse, chain);
        assertEquals(429, secondResponse.getStatus());

        Thread.sleep(350);

        MockHttpServletRequest third = new MockHttpServletRequest("GET", RECIPES_PATH);
        third.setRemoteAddr(CLIENT_IP);
        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();
        shortWindowFilter.doFilterInternal(third, thirdResponse, chain);
        assertEquals(200, thirdResponse.getStatus());
    }

    @Test
    void doFilterInternal_ShouldApplyIndependently_AlongsideExistingRateLimitFilter() throws Exception {
        GlobalRateLimitFilter tightGlobalFilter = new GlobalRateLimitFilter(new ObjectMapper().findAndRegisterModules(), 3, WINDOW_MILLIS);
        RateLimitFilter loginFilter = new RateLimitFilter(new ObjectMapper().findAndRegisterModules());
        String loginPath = "/api/auth/login";

        FilterChain terminalChain = mock(FilterChain.class);
        MockHttpServletResponse response = null;

        // RateLimitFilter allows up to 10 login attempts; the tighter global cap of 3 trips first.
        for (int i = 0; i < 4; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", loginPath);
            request.setRemoteAddr(CLIENT_IP);
            response = new MockHttpServletResponse();
            MockHttpServletResponse finalResponse = response;
            tightGlobalFilter.doFilterInternal(request, finalResponse, (req, resp) ->
                    loginFilter.doFilterInternal((jakarta.servlet.http.HttpServletRequest) req,
                            (jakarta.servlet.http.HttpServletResponse) resp, terminalChain));
        }

        assertEquals(429, response.getStatus());
        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        assertEquals("RATE_LIMIT_EXCEEDED", body.get("error").get("errorCode").asText());
        verify(terminalChain, times(3)).doFilter(any(), any());
    }
}
