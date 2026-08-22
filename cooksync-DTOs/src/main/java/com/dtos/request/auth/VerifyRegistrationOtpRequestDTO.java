package com.dtos.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
        String email,

        @NotBlank(message = "OTP code cannot be blank")
        @Pattern(regexp = "^\\d{6}$", message = "OTP code must be exactly 6 digits")
        String code
) {
}
