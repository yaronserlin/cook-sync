package com.dtos.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for completing the forgot-password flow with a valid reset code.
 *
 * @param email the account email the reset code was sent to
 * @param code the 6-digit reset code issued via the forgot-password email
 * @param newPassword the new raw password, requiring uppercase, lowercase, numeric, and special characters
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/08/2026
 */
public record ResetPasswordRequestDTO(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Reset code cannot be blank")
        @Pattern(regexp = "^\\d{6}$", message = "Reset code must be exactly 6 digits")
        String code,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        @Size(max = 100, message = "Password cannot exceed 100 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String newPassword
) {
}
