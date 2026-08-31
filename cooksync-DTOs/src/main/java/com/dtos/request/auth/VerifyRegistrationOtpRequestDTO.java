package com.dtos.request.auth;

import com.dtos.validation.OtpCode;
import com.dtos.validation.ValidEmail;

/**
 * Data Transfer Object for completing registration by submitting the OTP code sent by email.
 *
 * @param email the email address the pending registration and OTP code belong to
 * @param code the 6-digit numeric OTP code received by email
 * @author Yaron Serlin
 * @version 1.1
 * @since 13/08/2026
 */
public record VerifyRegistrationOtpRequestDTO(
        @ValidEmail
        String email,

        @OtpCode
        String code
) {
}
