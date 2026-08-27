package com.cooksync_server.constants;

/**
 * Centralizes literal values used by the JWT authentication and authorization machinery:
 * the HTTP header name and value prefix carrying bearer tokens, and the Spring Security role
 * names granted to authenticated users.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** Name of the HTTP header carrying the bearer access token. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Prefix preceding the raw JWT within the {@link #AUTHORIZATION_HEADER} value. */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Spring Security granted-authority name assigned to administrator accounts. */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** Spring Security granted-authority name assigned to ordinary user accounts. */
    public static final String ROLE_USER = "ROLE_USER";
}
