package com.dtos.response.recipe;

/**
 * Data Transfer Object representing a single content block within a recipe description.
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
        String type,
        String text,
        String imageUrl,
        String caption,
        boolean isMachineTranslated
) {
}
