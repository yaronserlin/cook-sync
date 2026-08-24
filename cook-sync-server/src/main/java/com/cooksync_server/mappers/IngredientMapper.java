package com.cooksync_server.mappers;

import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.Unit;
import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.response.ingredient.IngredientResponse;
import com.dtos.response.unit.UnitResponse;

import java.math.BigDecimal;

/**
 * Mapper utility class converting between Ingredient entities, IngredientRequestDTO payloads,
 * and IngredientResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public final class IngredientMapper {

    private IngredientMapper() {
    }

    /**
     * Builds a new (unsaved) Ingredient entity from a creation request, its resolved parent
     * recipe, and its resolved unit. Extracted from duplicated construction logic that
     * previously lived independently in both {@code IngredientServiceImp} and
     * {@code RecipeServiceImp}.
     *
     * @param recipe the parent recipe this ingredient belongs to
     * @param request ingredient creation payload DTO
     * @param unit the resolved measurement unit referenced by {@code request.unitId()}
     * @return a new, unpersisted Ingredient entity
     */
    public static Ingredient fromRequest(Recipe recipe, IngredientRequestDTO request, Unit unit) {
        return Ingredient.builder()
                .recipe(recipe)
                .name(request.name())
                .quantity(BigDecimal.valueOf(request.quantity()))
                .unit(unit)
                .build();
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
