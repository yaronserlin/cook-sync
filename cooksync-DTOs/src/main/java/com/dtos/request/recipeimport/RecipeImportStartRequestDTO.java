package com.dtos.request.recipeimport;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for starting a smart recipe-import job.
 *
 * @param sourceType {@code "WEB"} or {@code "PHOTO"}
 * @param sourceUrl the recipe page's URL, required (and only meaningful) when {@code sourceType}
 *                  is {@code "WEB"}. Restricted to {@code https://} to keep the server-side fetch
 *                  it triggers from being pointed at a non-HTTPS internal endpoint; the server
 *                  additionally rejects URLs that resolve to a non-public IP address before
 *                  fetching (see {@code SsrfGuard} on the server side)
 * @param sourceImageUrls the Cloudinary URLs of one or more already-uploaded photos, in the order
 *                        they should be read (e.g. successive pages of a handwritten recipe),
 *                        required (and only meaningful) when {@code sourceType} is {@code "PHOTO"}.
 *                        Restricted to Cloudinary's own CDN host, since the server downloads each
 *                        one's raw bytes and forwards them to the extraction provider
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public record RecipeImportStartRequestDTO(
        @NotBlank(message = "Source type is required")
        @Pattern(regexp = "WEB|PHOTO", message = "Source type must be WEB or PHOTO")
        String sourceType,

        @Pattern(regexp = "https://.+", message = "Source URL must be an https:// URL")
        @Size(max = 2048, message = "Source URL must be at most 2048 characters")
        String sourceUrl,

        @Size(max = 5, message = "A single import can use at most 5 photos")
        List<@Pattern(regexp = "https://res\\.cloudinary\\.com/.+",
                message = "Source image URLs must be Cloudinary URLs") String> sourceImageUrls
) {
}
