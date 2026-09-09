package com.cooksync_server.recipeimport;

/**
 * One raw source photo handed to {@link RecipeExtractionProvider#extractFromImages}, paired with
 * its MIME type.
 *
 * @param bytes the raw image bytes
 * @param mimeType the image's MIME type, e.g. {@code "image/jpeg"}
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public record ImageInput(byte[] bytes, String mimeType) {
}
