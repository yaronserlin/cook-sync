package com.cooksync_server.constants;

/**
 * Centralizes the REST API route path literals that must otherwise be hand-duplicated between
 * controller mapping annotations and {@code SecurityConfig}'s permit-all rules, so the two
 * cannot silently drift out of sync.
 * <p>
 * The auth path fragments below are deliberately kept relative to {@link #AUTH_BASE} rather than
 * fully qualified: {@code AuthController} applies {@link #AUTH_BASE} as its class-level
 * {@code @RequestMapping} and each endpoint annotation references only its own suffix, exactly as
 * before this constant was introduced. Call sites that need the fully-qualified path (such as
 * {@code SecurityConfig}'s permit-all matcher list) simply concatenate {@link #AUTH_BASE} with the
 * desired suffix constant.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class ApiRoutes {

    private ApiRoutes() {
    }

    /** Root path prefix under which every REST endpoint in this API is mounted. */
    public static final String API_ROOT = "/api";

    /** Base path for every authentication and account-settings endpoint. */
    public static final String AUTH_BASE = API_ROOT + "/auth";

    /** Relative path (under {@link #AUTH_BASE}) for beginning registration. */
    public static final String AUTH_REGISTER = "/register";

    /** Relative path (under {@link #AUTH_BASE}) for logging in. */
    public static final String AUTH_LOGIN = "/login";

    /** Relative path (under {@link #AUTH_BASE}) for exchanging a refresh token. */
    public static final String AUTH_REFRESH_TOKEN = "/refresh-token";

    /** Relative path (under {@link #AUTH_BASE}) for beginning the forgot-password flow. */
    public static final String AUTH_FORGOT_PASSWORD = "/forgot-password";

    /** Relative path (under {@link #AUTH_BASE}) for completing the forgot-password flow. */
    public static final String AUTH_RESET_PASSWORD = "/reset-password";

    /** Relative path (under {@link #AUTH_BASE}) for confirming a registration OTP code. */
    public static final String AUTH_VERIFY_REGISTRATION_OTP = "/verify-registration-otp";

    /** Relative path (under {@link #AUTH_BASE}) for reissuing a registration OTP code. */
    public static final String AUTH_RESEND_REGISTRATION_OTP = "/resend-registration-otp";

    /**
     * Fully-qualified path for the public, unauthenticated app-config lookup (minimum supported
     * client version + download link) — must be reachable before login, since a client too old
     * to trust its own login flow still needs to learn it must update.
     */
    public static final String APP_CONFIG = API_ROOT + "/app-config";
}
