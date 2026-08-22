package com.dtos.response.ingredient;

import java.math.BigDecimal;
import com.dtos.response.unit.UnitResponse;

/**
 * Data Transfer Object representing an ingredient item in API responses.
 * Encapsulates unique identifier, ingredient name, quantity, recipe mapping, and measurement unit details.
 *
 * @param id unique identifier of the ingredient record
 * @param name display name of the ingredient
 * @param quantity numeric quantity amount
 * @param recipeId unique identifier of the parent recipe
 * @param unit detailed measurement unit response DTO
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record IngredientResponse(
        String id,
        String name,
        BigDecimal quantity,
        String recipeId,
        UnitResponse unit
) {
}
