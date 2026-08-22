package com.dtos.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

/**
 * Composed Bean Validation constraint enforcing the account password policy shared by every
 * new/changed-password field across the auth request DTOs: at least one uppercase letter, one
 * lowercase letter, one digit, and one special character from {@code @$!%*?&}. Delegates
 * entirely to the meta-annotated {@link Pattern} constraint rather than a dedicated validator.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 22/08/2026
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
)
public @interface StrongPassword {

    /** Validation failure message, defaulting to the standard password-policy explanation. */
    String message() default "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character";

    /** Validation groups this constraint belongs to. */
    Class<?>[] groups() default {};

    /** Payload metadata clients of the Bean Validation API may use to assign custom severity. */
    Class<? extends Payload>[] payload() default {};
}
