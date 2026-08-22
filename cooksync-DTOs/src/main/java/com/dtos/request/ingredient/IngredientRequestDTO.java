package com.dtos.request.ingredient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object for creating or updating a recipe ingredient record.
 * Encapsulates temporary identifiers, item name, numeric quantity, and unit associations.
 *
 * @param tmpId client-side transient identifier for list item tracking
 * @param name the ingredient display name, must not be blank
 * @param quantity the ingredient numeric amount, must be a positive number
 * @param unitId the unique identifier of the measurement unit, must not be null
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record IngredientRequestDTO(
        String tmpId,

        @NotBlank(message = "Ingredient name is required")
        String name,

        @Positive(message = "Quantity must be a positive number")
        double quantity,

        @NotNull(message = "Unit ID is required")
        String unitId
) {
}