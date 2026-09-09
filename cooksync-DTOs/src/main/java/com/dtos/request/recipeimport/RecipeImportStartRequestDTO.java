package com.dtos.request.recipeimport;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Data Transfer Object for starting a smart recipe-import job.
 *
 * @param sourceType {@code "WEB"} or {@code "PHOTO"}
 * @param sourceUrl the recipe page's URL, required (and only meaningful) when {@code sourceType}
 *                  is {@code "WEB"}
 * @param sourceImageUrls the Cloudinary URLs of one or more already-uploaded photos, in the order
 *                        they should be read (e.g. successive pages of a handwritten recipe),
 *                        required (and only meaningful) when {@code sourceType} is {@code "PHOTO"}
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public record RecipeImportStartRequestDTO(
        @NotBlank(message = "Source type is required")
        @Pattern(regexp = "WEB|PHOTO", message = "Source type must be WEB or PHOTO")
        String sourceType,

        String sourceUrl,

        List<String> sourceImageUrls
) {
}
