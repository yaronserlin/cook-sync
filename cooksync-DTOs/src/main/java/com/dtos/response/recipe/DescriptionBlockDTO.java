package com.dtos.response.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object representing a single content block within a recipe description. Also
 * reused as request input (see {@code RecipeCreateRequestDTO#descriptionBlocks}), so it carries
 * validation constraints even though most of its call sites are outbound responses.
 * Blocks are discriminated by type: TEXT blocks carry prose content, IMAGE blocks carry a URL and optional caption.
 * List order of blocks preserves the author's intended content sequence.
 *
 * @param type block discriminator: "TEXT" or "IMAGE"
 * @param text prose content, populated when type is TEXT
 * @param imageUrl image resource URL, populated when type is IMAGE
 * @param caption optional image caption, only meaningful when type is IMAGE
 * @param isMachineTranslated whether {@code text} was produced by on-demand machine translation
 *                            rather than the recipe's own authored text or a human-reviewed
 *                            translation, always false when type is IMAGE
 * @author Yaron Serlin
 * @version 1.1
 * @since 04/08/2026
 */
public record DescriptionBlockDTO(
        @NotBlank(message = "Description block type is required")
        @Pattern(regexp = "TEXT|IMAGE", message = "Description block type must be TEXT or IMAGE")
        String type,

        @Size(max = 5000, message = "Description block text must be at most 5000 characters")
        String text,

        @Size(max = 2048, message = "Description block image URL must be at most 2048 characters")
        String imageUrl,

        @Size(max = 300, message = "Description block caption must be at most 300 characters")
        String caption,

        boolean isMachineTranslated
) {
}
