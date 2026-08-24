package com.cooksync_server.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter for intercepting, logging, and redacting sensitive data in HTTP requests and responses.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Slf4j
@Component
public class RequestAndResponseLoggingFilter extends OncePerRequestFilter {

    /** Matches a JSON field whose name suggests a sensitive value, capturing everything up to
     *  and including its opening quote so the replacement can restore the field name verbatim. */
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(?i)(\"[^\"]*(password|token|secret|apiKey)[^\"]*\"\\s*:\\s*)\"[^\"]*\"");
    /** Replacement text for a {@link #SENSITIVE_JSON_FIELD} match: keeps the captured field name
     *  ({@code $1}), replaces its value. */
    private static final String REDACTED_JSON_VALUE = "$1\"***REDACTED***\"";

    /**
     * Wraps request/response streams to record diagnostic log lines with masked sensitive fields.
     *
     * Complexity:
     * Time: O(P) where P is payload length
     * Space: O(P)
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

        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024 * 1024);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            logRequest(wrappedRequest);
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logResponse(wrappedResponse, duration, request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String requestData = readBody(((ContentCachingRequestWrapper) request).getContentAsByteArray());
        String clientIp = request.getRemoteAddr();

        log.info("REQUEST | method={} uri={} ip={}", method, uri + queryString, clientIp);

        if (log.isDebugEnabled()) {
            log.debug("REQUEST payload={}", truncatePayload(requestData));
        }
    }

    private void logResponse(HttpServletResponse response, long duration, String requestUri) {
        String responseData = readBody(((ContentCachingResponseWrapper) response).getContentAsByteArray());
        int status = response.getStatus();

        log.info("RESPONSE | status={} uri={} durationMs={}", status, requestUri, duration);

        if (log.isDebugEnabled()) {
            log.debug("RESPONSE payload={}", truncatePayload(responseData));
        }
    }

    private String readBody(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String truncatePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return "<empty>";
        }
        String compact = payload.replace("\n", " ").replace("\r", " ").trim();
        compact = SENSITIVE_JSON_FIELD.matcher(compact).replaceAll(REDACTED_JSON_VALUE);
        if (compact.length() <= 220) {
            return compact;
        }
        return compact.substring(0, 217) + "...";
    }
}
