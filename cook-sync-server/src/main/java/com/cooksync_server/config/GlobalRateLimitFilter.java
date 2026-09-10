package com.cooksync_server.config;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cooksync_server.constants.ApiRoutes;
import com.dtos.response.ApiResponse;
import com.dtos.response.errors.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Blanket per-IP request-volume cap applied across the whole API, independent of and in addition
 * to {@link RateLimitFilter}'s narrower per-path rules on specific auth endpoints. Where that
 * filter answers "is this IP hammering this one sensitive path?", this one answers "is this IP
 * too hot overall?" - a floor against scripted abuse/DoS on every endpoint the narrow filter
 * doesn't cover (recipe browsing, search, image uploads, etc.), tracked independently of
 * {@link RateLimitFilter}'s own counters so the two never interfere with each other.
 * <p>
 * {@code /actuator/health} and {@link ApiRoutes#APP_CONFIG} are exempt: the former is polled
 * frequently by Render's/Docker's health check, and a false 429 there risks the platform killing
 * a healthy instance; the latter must always be reachable, even by an outdated client that hasn't
 * logged in yet, so it can learn it needs to update.
 * <p>
 * In-memory and per-instance, for the same reason documented on {@link RateLimitFilter}: correct
 * for CookSync's single-instance Render deployment. If the server is ever horizontally scaled,
 * this would need a shared store (e.g. Redis) instead.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
@Slf4j
@Component
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    /** Infra/operational paths that must never be throttled. */
    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/actuator/health",
            ApiRoutes.APP_CONFIG
    );

    /**
     * How often {@link #evictStaleWindows()} sweeps for idle per-IP counters, and the floor for
     * how long one is kept before being reclaimed. Kept as a fixed cadence independent of
     * {@link #windowMillis} - {@code @Scheduled} needs a compile-time constant - but the actual
     * eviction cutoff in {@link #evictStaleWindows()} widens past this floor whenever
     * {@code windowMillis} is configured larger than it, so a counter is never reclaimed before
     * its own window would have naturally expired.
     */
    private static final long STALE_SWEEP_INTERVAL_MILLIS = 600_000;

    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowMillis;

    /** Live per-IP counters, keyed by remote address alone (not by path, unlike {@link RateLimitFilter}). */
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Mutable request count within the current fixed window for one client IP. */
    private static final class Window {
        private long startMillis = System.currentTimeMillis();
        private int count = 0;
    }

    /**
     * @param objectMapper Spring Boot's auto-configured Jackson mapper, used to serialize the 429 body
     * @param maxRequests requests allowed per IP within {@code windowMillis} (default: 120)
     * @param windowMillis fixed-window length in milliseconds (default: 60000, i.e. one minute)
     */
    public GlobalRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${rate-limit.global.max-requests:120}") int maxRequests,
            @Value("${rate-limit.global.window-millis:60000}") long windowMillis) {
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /**
     * Skips exempt infra paths entirely so they're never counted against, or rejected by, the
     * global cap.
     *
     * @param request current HTTP request
     * @return {@code true} if this request's path is exempt from rate limiting
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXEMPT_PATHS.contains(request.getRequestURI());
    }

    /**
     * Applies the blanket per-IP limit before letting the request through.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain target filter chain
     * @throws ServletException if filter error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isAllowed(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Global rate limit exceeded: ip={}", request.getRemoteAddr());
        writeTooManyRequests(request, response);
    }

    /**
     * Checks and updates the fixed-window counter for this request's IP, resetting it once
     * {@link #windowMillis} has elapsed since the window started.
     *
     * @param request current HTTP request, used for its remote address
     * @return {@code true} if the request is within the limit, {@code false} if it should be rejected
     */
    private boolean isAllowed(HttpServletRequest request) {
        String key = request.getRemoteAddr();
        Window window = windows.computeIfAbsent(key, k -> new Window());

        synchronized (window) {
            long now = System.currentTimeMillis();
            if (now - window.startMillis >= windowMillis) {
                window.startMillis = now;
                window.count = 0;
            }
            window.count++;
            return window.count <= maxRequests;
        }
    }

    /**
     * Writes a 429 response in the same {@code ApiResponse}/{@code ApiErrorResponse} envelope
     * {@link RateLimitFilter} and {@code GlobalExceptionHandler} use. This filter runs ahead of
     * Spring MVC's exception handling, so it cannot rely on that advisor and writes the body
     * directly instead.
     *
     * @param request current HTTP request, used for the error body's {@code path}
     * @param response current HTTP response, written to directly
     * @throws IOException if writing the response body fails
     */
    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please try again later.",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getWriter(), ApiResponse.error(error, null));
    }

    /**
     * Reclaims idle per-IP counters so {@link #windows} doesn't grow unbounded under sustained
     * traffic from many distinct IPs. The cutoff widens past {@link #STALE_SWEEP_INTERVAL_MILLIS}
     * whenever {@link #windowMillis} is configured larger than it, so a counter for an IP that's
     * merely spread its requests across a long window is never reclaimed - and its count silently
     * reset - before that window would have elapsed on its own.
     */
    @Scheduled(fixedRate = STALE_SWEEP_INTERVAL_MILLIS)
    void evictStaleWindows() {
        long staleAfterMillis = Math.max(STALE_SWEEP_INTERVAL_MILLIS, windowMillis * 2);
        long cutoff = System.currentTimeMillis() - staleAfterMillis;
        windows.values().removeIf(window -> window.startMillis < cutoff);
    }
}
