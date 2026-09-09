package com.dtos.request.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Data Transfer Object for changing only a recipe's visibility, without
 * resubmitting the rest of the recipe's fields.
 *
 * @param visibility recipe visibility setting (PUBLIC, PRIVATE), must not be blank
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record RecipeVisibilityUpdateRequestDTO(
        @NotBlank(message = "Visibility is required (PUBLIC, PRIVATE)")
        @Pattern(regexp = "PUBLIC|PRIVATE", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Visibility must be PUBLIC or PRIVATE")
        String visibility
) {
}
