package com.dtos.request.auth;

import com.dtos.validation.OtpCode;

/**
 * Data Transfer Object for completing a self-service email-address change by submitting the OTP
 * code emailed to the requested new address. The caller is identified from their authenticated
 * session, and the pending new email is looked up server-side from the request that triggered
 * {@code PUT /api/auth/email}, so neither needs to be resubmitted here.
 *
 * @param code the 6-digit OTP code received at the new email address
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
public record VerifyEmailChangeOtpRequestDTO(
        @OtpCode
        String code
) {
}
