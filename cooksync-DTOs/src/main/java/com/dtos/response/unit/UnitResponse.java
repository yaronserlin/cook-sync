package com.dtos.response.unit;

/**
 * Data Transfer Object representing a measurement unit in API responses.
 * Encapsulates unit unique identifier, symbol code, display name, and audit timestamps.
 *
 * @param id unique identifier of the unit record
 * @param code short symbol code of the measurement unit (e.g. "kg", "tsp")
 * @param name full display name of the measurement unit (e.g. "Kilogram", "Teaspoon")
 * @param createdAt ISO formatted creation timestamp string
 * @param updatedAt ISO formatted last update timestamp string
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record UnitResponse(
        String id,
        String code,
        String name,
        String createdAt,
        String updatedAt
) {
}
