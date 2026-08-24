package com.dtos.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Composed Bean Validation constraint for a one-time verification code field, shared by every
 * request DTO that completes an OTP-style flow (registration verification, forgot-password
 * reset, email-change verification): required, and exactly 6 digits. Delegates entirely to the
 * meta-annotated {@link NotBlank} and {@link Pattern} constraints rather than a dedicated
 * validator.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@NotBlank(message = "OTP code cannot be blank")
@Pattern(regexp = "^\\d{6}$", message = "OTP code must be exactly 6 digits")
public @interface OtpCode {

    /**
     * Validation failure message, defaulting to the digit-format explanation.
     */
    String message() default "OTP code must be exactly 6 digits";

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
