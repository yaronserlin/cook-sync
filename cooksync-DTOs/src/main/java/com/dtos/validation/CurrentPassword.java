package com.dtos.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Composed Bean Validation constraint for a re-authentication
 * {@code currentPassword} field, shared by every request DTO that requires the
 * caller to prove their current password before a sensitive account change
 * (password change, email change, account deletion). Delegates entirely to the
 * meta-annotated {@link NotBlank} and {@link Size} constraints rather than a
 * dedicated validator.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 22/08/2026
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@NotBlank(message = "Current password is required")
@Size(max = 100, message = "Current password cannot exceed 100 characters")
public @interface CurrentPassword {

    /**
     * Validation failure message, defaulting to the blank-field explanation.
     */
    String message() default "Current password is required";

    /**
     * Validation groups this constraint belongs to.
     */
    Class<?>[] groups() default {};

    /**
     * Payload metadata clients of the Bean Validation API may use to assign
     * custom severity.
     */
    Class<? extends Payload>[] payload() default {};
}
