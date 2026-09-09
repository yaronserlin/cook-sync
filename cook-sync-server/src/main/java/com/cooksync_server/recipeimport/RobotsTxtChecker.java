package com.cooksync_server.recipeimport;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import lombok.extern.slf4j.Slf4j;

/**
 * A basic, best-effort {@code robots.txt} check before a recipe-import job scrapes a user-pasted
 * URL: fetches the source site's {@code /robots.txt} and honors any {@code Disallow} rule under a
 * {@code User-agent: *} block for the requested path.
 *
 * <p><b>This is not a legal/copyright review</b> — it's the same "good internet citizen" courtesy
 * search engines and reputable scraping tools extend, nothing more. A site with no
 * {@code robots.txt} (or one this can't reach) is treated as allowing access, matching the
 * standard convention that absence of the file means no crawling restrictions are declared.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@Slf4j
@Component
public class RobotsTxtChecker {

    private static final int TIMEOUT_MS = 8_000;

    private final RestClient client = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .build();

    /**
     * Checks whether {@code url}'s own path is disallowed for all crawlers by its site's
     * {@code robots.txt}.
     *
     * @param url the target recipe page URL
     * @return {@code true} if importing is allowed (no {@code robots.txt}, no matching
     *         {@code Disallow} rule, or the check itself failed) — {@code false} only when a
     *         {@code User-agent: *} block explicitly disallows this exact path
     */
    public boolean isAllowed(String url) {
        try {
            URI uri = URI.create(url);
            URL robotsUrl = URI.create(uri.getScheme() + "://" + uri.getAuthority() + "/robots.txt").toURL();
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();

            String robotsTxt = client.get().uri(robotsUrl.toURI()).retrieve().body(String.class);
            if (robotsTxt == null) {
                return true;
            }
            return !isDisallowed(robotsTxt, path);
        } catch (RestClientResponseException e) {
            // No robots.txt (404) or the site rejected the request some other way — proceed;
            // absence of the file is the standard "no restrictions declared" case.
            return true;
        } catch (Exception e) {
            log.warn("robots.txt check failed for {}: {}", url, e.getMessage());
            return true;
        }
    }

    /**
     * Parses the {@code User-agent: *} block(s) of a robots.txt body and checks whether any of
     * their {@code Disallow} rules is a prefix of {@code path}.
     *
     * @param robotsTxt the fetched robots.txt content
     * @param path the request path to check (e.g. {@code "/recipes/lemon-chicken"})
     * @return {@code true} if a matching {@code Disallow} rule under {@code User-agent: *} applies
     */
    static boolean isDisallowed(String robotsTxt, String path) {
        List<String> disallowPrefixes = new ArrayList<>();
        boolean inWildcardBlock = false;

        for (String rawLine : robotsTxt.split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String field = parts[0].trim().toLowerCase(java.util.Locale.ROOT);
            String value = parts[1].trim();

            if (field.equals("user-agent")) {
                inWildcardBlock = value.equals("*");
            } else if (inWildcardBlock && field.equals("disallow") && !value.isEmpty()) {
                disallowPrefixes.add(value);
            }
        }

        return disallowPrefixes.stream().anyMatch(path::startsWith);
    }

    private static String stripComment(String line) {
        int hashIndex = line.indexOf('#');
        return hashIndex >= 0 ? line.substring(0, hashIndex) : line;
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return factory;
    }
}
