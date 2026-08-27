package com.cooksync.app.util.constants;

/**
 * URL path suffixes for the authentication endpoints reachable without an access token attached,
 * previously duplicated between {@code ApiService}'s {@code @POST} Retrofit annotations and
 * {@code AuthInterceptor}'s {@code PUBLIC_PATH_SUFFIXES} array. Declared as {@code public static
 * final String} so they remain usable as Retrofit annotation values, which require compile-time
 * constant expressions.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class ApiEndpoints {

    /** Path for {@code POST /api/auth/login}. */
    public static final String LOGIN = "api/auth/login";

    /** Path for {@code POST /api/auth/register}. */
    public static final String REGISTER = "api/auth/register";

    /** Path for {@code POST /api/auth/refresh-token}. */
    public static final String REFRESH_TOKEN = "api/auth/refresh-token";

    /** Path for {@code POST /api/auth/verify-registration-otp}. */
    public static final String VERIFY_REGISTRATION_OTP = "api/auth/verify-registration-otp";

    /** Path for {@code POST /api/auth/resend-registration-otp}. */
    public static final String RESEND_REGISTRATION_OTP = "api/auth/resend-registration-otp";

    private ApiEndpoints() {}
}
