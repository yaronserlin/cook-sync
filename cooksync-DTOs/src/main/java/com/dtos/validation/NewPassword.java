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
 * Composed Bean Validation constraint for a new-password field, shared by every request DTO
 * that sets a fresh account password (registration, password change, and forgot-password reset):
 * required, between 6 and 100 characters, and meeting the {@link StrongPassword} character-class
 * policy. Delegates entirely to the meta-annotated {@link NotBlank}, {@link Size}, and
 * {@link StrongPassword} constraints rather than a dedicated validator.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 31/08/2026
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@NotBlank(message = "Password cannot be blank")
@Size(min = 6, message = "Password must be at least 6 characters long")
@Size(max = 100, message = "Password cannot exceed 100 characters")
@StrongPassword
public @interface NewPassword {

    /**
     * Validation failure message, defaulting to the blank-field explanation.
     */
    String message() default "Password cannot be blank";

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
