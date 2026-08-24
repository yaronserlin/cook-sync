package com.dtos.request.auth;

import com.dtos.validation.OtpCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for completing registration by submitting the OTP code sent by email.
 *
 * @param email the email address the pending registration and OTP code belong to
 * @param code the 6-digit numeric OTP code received by email
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
public record VerifyRegistrationOtpRequestDTO(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @OtpCode
        String code
) {
}
