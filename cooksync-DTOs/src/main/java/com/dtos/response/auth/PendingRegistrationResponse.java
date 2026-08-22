package com.dtos.response.auth;

/**
 * Data Transfer Object returned after a registration form submission that requires email OTP verification.
 * No authentication tokens are issued at this stage; the account only becomes usable once the OTP is verified.
 *
 * @param email the email address the OTP code was sent to
 * @param otpExpiresInSeconds the number of seconds until the issued OTP code expires
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public record PendingRegistrationResponse(
        String email,
        long otpExpiresInSeconds
) {
}
