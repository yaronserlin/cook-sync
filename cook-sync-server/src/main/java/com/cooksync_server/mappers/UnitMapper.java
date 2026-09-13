package com.cooksync_server.mappers;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.entities.Unit;
import com.dtos.response.unit.UnitResponse;

/**
 * Mapper utility class transforming Unit entities into UnitResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public final class UnitMapper {

    private UnitMapper() {
    }

    /**
     * Converts a Unit entity into a UnitResponse DTO.
     *
     * @param unit target Unit entity instance
     * @return populated UnitResponse instance or null
     */
    public static UnitResponse toResponse(Unit unit) {
        if (unit == null) {
            return null;
        }
        String created = MapperUtils.toIsoStringOrNull(unit.getCreatedAt());
        String updated = MapperUtils.toIsoStringOrNull(unit.getUpdatedAt());
        String name = TranslationAccess.resolve(ContentTranslation.EntityType.UNIT_NAME, unit.getId(), unit.getName(), "en").value();
        String namePlural = TranslationAccess.resolve(ContentTranslation.EntityType.UNIT_NAME_PLURAL, unit.getId(), unit.getNamePlural(), "en").value();
        return new UnitResponse(unit.getId(), unit.getCode(), name, namePlural, created, updated);
    }

    /**
     * Converts a Unit entity into a UnitResponse DTO, reading its translated name and plural name
     * from an already-resolved {@link RecipeTranslationBundle} instead of resolving them
     * independently — used when this unit is part of a recipe-wide coordinated translation
     * attempt (see {@link RecipeTranslationCoordinator}).
     *
     * @param unit target Unit entity instance
     * @param bundle the recipe's resolved translation bundle
     * @return populated UnitResponse instance or null
     */
    static UnitResponse toResponse(Unit unit, RecipeTranslationBundle bundle) {
        if (unit == null) {
            return null;
        }
        String created = MapperUtils.toIsoStringOrNull(unit.getCreatedAt());
        String updated = MapperUtils.toIsoStringOrNull(unit.getUpdatedAt());
        String name = bundle.valueOf(ContentTranslation.EntityType.UNIT_NAME, unit.getId(), unit.getName());
        String namePlural = bundle.valueOf(ContentTranslation.EntityType.UNIT_NAME_PLURAL, unit.getId(), unit.getNamePlural());
        return new UnitResponse(unit.getId(), unit.getCode(), name, namePlural, created, updated);
    }
}
