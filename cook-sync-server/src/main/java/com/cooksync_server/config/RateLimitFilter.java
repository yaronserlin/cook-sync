package com.cooksync_server.config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Per-IP request throttle for the public (unauthenticated) auth endpoints most exposed to abuse:
 * login/registration brute-forcing, and OTP-email spam ("email bombing") via forgot-password and
 * resend-registration-otp. Each guarded path tracks its own fixed-window counter per client IP,
 * independent of the others, so a burst against one endpoint doesn't affect the limit on another.
 * <p>
 * This is a coarse, defense-in-depth layer on top of the existing per-record OTP attempt counters
 * (see {@code TooManyOtpAttemptsException}), which guard how many times a single already-issued
 * code can be guessed but do nothing to limit request <em>volume</em> against these endpoints.
 * <p>
 * In-memory and per-instance: correct for CookSync's single-instance Render deployment, where
 * {@code server.forward-headers-strategy=native} (see {@code application-prod.properties}) makes
 * {@link HttpServletRequest#getRemoteAddr()} resolve to the real client IP rather than Render's
 * edge proxy. If the server is ever horizontally scaled, this would need a shared store (e.g.
 * Redis) instead, since each instance would otherwise track its own independent counters.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 01/09/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    /** One fixed-window throttle rule: at most {@code maxRequests} within {@code windowMillis}. */
    private record Rule(int maxRequests, long windowMillis) {
    }

    /**
     * Guarded paths and their limits. {@link ApiRoutes#AUTH_FORGOT_PASSWORD} and
     * {@link ApiRoutes#AUTH_RESEND_REGISTRATION_OTP} get the tightest limits since each request
     * sends a real email to a caller-supplied (not necessarily the caller's own) address.
     * {@link ApiRoutes#AUTH_REFRESH_TOKEN} is deliberately not guarded here: refresh tokens are
     * long, unguessable JWTs (not brute-forceable like a password or a 6-digit OTP), and it is
     * called automatically and frequently by every legitimate signed-in client.
     */
    private static final Map<String, Rule> RULES = Map.of(
            ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_LOGIN, new Rule(10, 60_000),
            ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_REGISTER, new Rule(5, 60_000),
            ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_FORGOT_PASSWORD, new Rule(3, 300_000),
            ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_RESET_PASSWORD, new Rule(10, 60_000),
            ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_VERIFY_REGISTRATION_OTP, new Rule(10, 60_000),
            ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_RESEND_REGISTRATION_OTP, new Rule(3, 300_000)
    );

    /** How long an idle per-(IP, path) counter is kept before {@link #evictStaleWindows()} reclaims it. */
    private static final long STALE_AFTER_MILLIS = 600_000;

    private final ObjectMapper objectMapper;

    /** Live per-(IP, path) counters, keyed by {@code "<remoteAddr>|<requestURI>"}. */
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Mutable request count within the current fixed window for one (IP, path) pair. */
    private static final class Window {
        private long startMillis = System.currentTimeMillis();
        private int count = 0;
    }

    /**
     * Applies the matching {@link Rule} for this request's path, if any, before letting the
     * request through; paths with no rule pass straight through untouched.
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

        Rule rule = RULES.get(request.getRequestURI());
        if (rule == null || isAllowed(request, rule)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded: ip={} uri={}", request.getRemoteAddr(), request.getRequestURI());
        writeTooManyRequests(request, response);
    }

    /**
     * Checks and updates the fixed-window counter for this request's (IP, path) pair, resetting
     * it once {@link Rule#windowMillis()} has elapsed since the window started.
     *
     * @param request current HTTP request, used for its remote address
     * @param rule the limit to enforce for this request's path
     * @return {@code true} if the request is within the limit, {@code false} if it should be rejected
     */
    private boolean isAllowed(HttpServletRequest request, Rule rule) {
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        Window window = windows.computeIfAbsent(key, k -> new Window());

        synchronized (window) {
            long now = System.currentTimeMillis();
            if (now - window.startMillis >= rule.windowMillis()) {
                window.startMillis = now;
                window.count = 0;
            }
            window.count++;
            return window.count <= rule.maxRequests();
        }
    }

    /**
     * Writes a 429 response in the same {@code ApiResponse}/{@code ApiErrorResponse} envelope
     * {@code GlobalExceptionHandler} uses. This filter runs ahead of Spring MVC's exception
     * handling, so it cannot rely on that advisor and writes the body directly instead - the same
     * approach {@link JwtAuthenticationEntryPoint} already takes for 401s.
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
     * Reclaims idle per-(IP, path) counters so {@link #windows} doesn't grow unbounded under
     * sustained traffic from many distinct IPs.
     */
    @Scheduled(fixedRate = STALE_AFTER_MILLIS)
    void evictStaleWindows() {
        long cutoff = System.currentTimeMillis() - STALE_AFTER_MILLIS;
        windows.values().removeIf(window -> window.startMillis < cutoff);
    }
}
