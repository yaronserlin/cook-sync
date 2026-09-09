package com.cooksync_server.recipeimport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import org.springframework.stereotype.Component;

/**
 * Rejects a user-supplied recipe-import source URL before {@link WebPageTextExtractor} or
 * {@link RobotsTxtChecker} make any request to it, closing the server-side request forgery
 * (SSRF) path a {@code sourceUrl} would otherwise open into the server's own network: an
 * authenticated user could otherwise point a WEB import at an internal service, {@code
 * localhost}, or a cloud metadata endpoint (e.g. {@code 169.254.169.254}, covered by the
 * link-local check below), and this server would fetch it on their behalf.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
@Component
public class SsrfGuard {

    /**
     * Resolves {@code url}'s host and verifies both the scheme and every resolved address are
     * safe to connect to.
     *
     * @param url the user-supplied URL about to be fetched
     * @throws IOException if the URL isn't {@code https}, has no host, its host can't be
     * resolved, or any of its resolved addresses is loopback, link-local, site-local (RFC 1918),
     * multicast, or the wildcard address
     */
    public void assertPubliclyRoutable(String url) throws IOException {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("Only https:// URLs are allowed");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IOException("URL has no host");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IOException("Could not resolve host: " + host, e);
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()
                    || address.isAnyLocalAddress()) {
                throw new IOException("URL resolves to a non-public address: " + host);
            }
        }
    }
}
