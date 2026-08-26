package com.dtos.request.ingredient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Data Transfer Object for creating or updating a recipe ingredient record, submitted as one entry
 * of a {@code RecipeCreateRequestDTO}'s ingredient list.
 *
 * @param tmpId client-generated UUID string identifying this not-yet-persisted ingredient within
 *              the request; the server maps it to the ingredient it saves so that any sibling
 *              {@code InstructionRequestDTO#ingredientIds} referencing the same value can be
 *              linked to it once persisted
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