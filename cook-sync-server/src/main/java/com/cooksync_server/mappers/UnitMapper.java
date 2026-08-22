package com.cooksync_server.mappers;

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
        return new UnitResponse(unit.getId(), unit.getCode(), unit.getName(), created, updated);
    }
}
