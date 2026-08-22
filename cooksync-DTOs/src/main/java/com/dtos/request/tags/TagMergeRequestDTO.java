package com.dtos.request.tags;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for administrative tag consolidation operations.
 * Specifies the source duplicate tag identifier and target canonical tag identifier.
 *
 * @param sourceTagId the unique identifier of the duplicate tag to be merged and removed
 * @param targetTagId the unique identifier of the canonical tag to retain
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record TagMergeRequestDTO(
        @NotBlank(message = "Source tag id is required")
        String sourceTagId,

        @NotBlank(message = "Target tag id is required")
        String targetTagId
) {
}
