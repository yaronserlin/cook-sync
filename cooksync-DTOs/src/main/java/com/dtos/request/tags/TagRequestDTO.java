package com.dtos.request.tags;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating (or finding an existing) category tag.
 * Encapsulates tag label validation constraints.
 *
 * @param name the display name of the tag, must be between 2 and 50 characters
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public record TagRequestDTO(
        @NotBlank(message = "Tag name is required")
        @Size(min = 2, max = 50, message = "Tag name must be between 2 and 50 characters")
        String name
) {
}