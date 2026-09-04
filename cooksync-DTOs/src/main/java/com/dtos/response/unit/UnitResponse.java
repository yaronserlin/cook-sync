package com.dtos.response.unit;

/**
 * Data Transfer Object representing a measurement unit in API responses.
 * Encapsulates unit unique identifier, symbol code, singular/plural display names, and audit
 * timestamps.
 *
 * @param id unique identifier of the unit record
 * @param code short symbol code of the measurement unit (e.g. "kg", "tsp") — never translated,
 *             the same across every locale
 * @param name full singular display name of the measurement unit (e.g. "Kilogram", "Teaspoon"),
 *             translated to the request's locale where available
 * @param namePlural full plural display name of the measurement unit (e.g. "Kilograms",
 *                   "Teaspoons"), translated to the request's locale where available — the form
 *                   to show whenever the paired quantity isn't exactly one
 * @param createdAt ISO formatted creation timestamp string
 * @param updatedAt ISO formatted last update timestamp string
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public record UnitResponse(
        String id,
        String code,
        String name,
        String namePlural,
        String createdAt,
        String updatedAt
) {
}
