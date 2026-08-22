package com.dtos.response.tags;

/**
 * Data Transfer Object representing a category or ingredient tag in API responses.
 * Encapsulates tag unique identifier, display label name, and audit timestamps.
 *
 * @param id unique identifier of the tag record
 * @param name display label of the tag
 * @param createdAt ISO formatted creation timestamp string
 * @param updatedAt ISO formatted last update timestamp string
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record TagResponse(
        String id,
        String name,
        String createdAt,
        String updatedAt
) {
}
