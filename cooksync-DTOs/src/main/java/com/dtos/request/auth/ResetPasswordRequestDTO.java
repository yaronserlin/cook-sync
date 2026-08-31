package com.dtos.request.auth;

import com.dtos.validation.NewPassword;
import com.dtos.validation.OtpCode;
import com.dtos.validation.ValidEmail;

/**
 * Data Transfer Object for completing the forgot-password flow with a valid reset code.
 *
 * @param email the account email the reset code was sent to
 * @param code the 6-digit reset code issued via the forgot-password email
 * @param newPassword the new raw password, requiring uppercase, lowercase, numeric, and special characters
 * @author Yaron Serlin
 * @version 1.1
 * @since 05/08/2026
 */
public record ResetPasswordRequestDTO(
        @ValidEmail
        String email,

        @OtpCode
        String code,

        @NewPassword
        String newPassword
) {
}
