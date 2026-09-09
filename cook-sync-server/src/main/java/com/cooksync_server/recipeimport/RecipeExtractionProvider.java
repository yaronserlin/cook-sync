package com.cooksync_server.recipeimport;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over "who actually turns raw recipe content into structured data" — the same seam
 * pattern as {@code com.cooksync_server.translation.TranslationProvider} from auto-translation,
 * so swapping the underlying model/vendor later means adding a new implementation, not touching
 * {@code RecipeImportServiceImp}'s job-lifecycle logic.
 *
 * <p>Both extraction paths (web-page text and a photo) funnel through this one interface — Gemini
 * (the only implementation today, {@link GeminiRecipeExtractionProvider}) natively accepts an
 * image alongside a text prompt, so no separate OCR step/provider is needed the way an
 * OCR-then-LLM pipeline would require.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public interface RecipeExtractionProvider {

    /**
     * Extracts recipe data from a web page's cleaned text content.
     *
     * @param pageText the page's main readable text (nav/scripts/ads already stripped)
     * @param unitCodes the real {@code units.code} values to ground the model's unit choice in
     * @return the extracted recipe, or empty if the model determined the content isn't a recipe
     * @throws ExtractionServiceException if the underlying call itself failed (network error,
     *         non-2xx response, quota exhausted) — distinct from "not a recipe"
     */
    Optional<ExtractedRecipe> extractFromWebPage(String pageText, List<String> unitCodes);

    /**
     * Extracts recipe data directly from one or more photos (camera captures or gallery picks) of
     * a recipe — a cookbook page, a handwritten note, etc. Multiple photos are read together as
     * one recipe (e.g. successive pages of a handwritten recipe, or an ingredients list and a
     * method written on separate cards), in the given order.
     *
     * @param images the source photos, in reading order
     * @param unitCodes the real {@code units.code} values to ground the model's unit choice in
     * @return the extracted recipe, or empty if the model determined the content isn't a recipe
     * @throws ExtractionServiceException if the underlying call itself failed (network error,
     *         non-2xx response, quota exhausted) — distinct from "not a recipe"
     */
    Optional<ExtractedRecipe> extractFromImages(List<ImageInput> images, List<String> unitCodes);
}
