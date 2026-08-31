package com.dtos.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Composed Bean Validation constraint for an email-address field, shared by every request DTO
 * that accepts an email (login, registration, forgot-password, OTP resend/verification, and
 * email-change): required, well-formed, and at most 255 characters. Delegates entirely to the
 * meta-annotated {@link NotBlank}, {@link Email}, and {@link Size} constraints rather than a
 * dedicated validator.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 31/08/2026
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@NotBlank(message = "Email cannot be blank")
@Email(message = "Email should be valid")
@Size(max = 255, message = "Email cannot exceed 255 characters")
public @interface ValidEmail {

    /**
     * Validation failure message, defaulting to the well-formedness explanation.
     */
    String message() default "Email should be valid";

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
