package com.dtos.request.unit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating or updating a measurement unit definition.
 * Encapsulates full unit display name and short unit symbol code.
 *
 * @param name the full name of the measurement unit, between 2 and 50 characters
 * @param code the short symbol code representing the unit, up to 10 characters
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record UnitRequestDTO(
        @NotBlank(message = "Unit name cannot be blank")
        @Size(min = 2, max = 50, message = "Unit name must be between 2 and 50 characters")
        String name,

        @NotBlank(message = "Unit code cannot be blank")
        @Size(max = 10, message = "Unit code must be at most 10 characters")
        String code
) {
}