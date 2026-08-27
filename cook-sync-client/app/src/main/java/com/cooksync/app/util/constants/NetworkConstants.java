package com.cooksync.app.util.constants;

/**
 * Shared HTTP constants for the networking layer, previously duplicated identically between
 * {@link com.cooksync.app.data.datasource.remote.AuthInterceptor} and
 * {@link com.cooksync.app.data.datasource.remote.TokenAuthenticator}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class NetworkConstants {

    /** Name of the HTTP header carrying the bearer access token. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Prefix prepended to the access token when building the {@link #AUTHORIZATION_HEADER} value. */
    public static final String BEARER_PREFIX = "Bearer ";

    private NetworkConstants() {}
}
