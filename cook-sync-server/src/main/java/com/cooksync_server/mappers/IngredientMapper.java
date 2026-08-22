package com.cooksync_server.mappers;

import com.cooksync_server.entities.Ingredient;
import com.dtos.response.ingredient.IngredientResponse;
import com.dtos.response.unit.UnitResponse;

/**
 * Mapper utility class converting Ingredient entities into IngredientResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public final class IngredientMapper {

    private IngredientMapper() {
    }

    /**
     * Converts an Ingredient entity into an IngredientResponse DTO.
     *
     * @param entity target Ingredient entity
     * @return populated IngredientResponse DTO instance or null
     */
    public static IngredientResponse toResponse(Ingredient entity) {
        if (entity == null) {
            return null;
        }

        UnitResponse unitResponse = UnitMapper.toResponse(entity.getUnit());

        String recipeId = null;
        if (entity.getRecipe() != null) {
            recipeId = entity.getRecipe().getId();
        }

        return new IngredientResponse(
                entity.getId(),
                entity.getName(),
                entity.getQuantity(),
                recipeId,
                unitResponse
        );
    }
}
