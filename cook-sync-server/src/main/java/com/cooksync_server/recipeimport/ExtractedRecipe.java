package com.cooksync_server.recipeimport;

import java.math.BigDecimal;
import java.util.List;

/**
 * A recipe as extracted by a {@link RecipeExtractionProvider}, before its ingredient unit codes
 * are resolved against the real {@code units} table and it's handed to {@code RecipeServiceImp}
 * as a {@code RecipeCreateRequestDTO}. Deliberately not a shared DTO in {@code cooksync-DTOs}:
 * nothing outside this extraction pipeline ever sees this shape — the client only ever sees the
 * real {@code RecipeResponse} produced once extraction succeeds and the recipe is actually
 * created.
 *
 * @param title the recipe's title
 * @param description a short intro paragraph, or blank if the source had none
 * @param difficulty one of {@code EASY}, {@code MEDIUM}, {@code HARD}
 * @param prepTimeMinutes preparation time in minutes
 * @param cookTimeMinutes active cooking time in minutes
 * @param servings recommended yield count
 * @param ingredients the recipe's ingredients, in source order
 * @param instructions the recipe's steps, in order
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public record ExtractedRecipe(
        String title,
        String description,
        String difficulty,
        int prepTimeMinutes,
        int cookTimeMinutes,
        int servings,
        List<ExtractedIngredient> ingredients,
        List<String> instructions
) {

    /**
     * One extracted ingredient line.
     *
     * @param name the ingredient's display name
     * @param quantity the numeric amount
     * @param unitCode the extraction model's best-guess unit code — grounded, via the extraction
     *                 prompt, to the app's real {@code units.code} values, but not yet verified
     *                 against the database; {@link com.cooksync_server.services.RecipeImportServiceImp}
     *                 resolves it to a real unit id, falling back to "piece" if it doesn't match
     */
    public record ExtractedIngredient(String name, BigDecimal quantity, String unitCode) {
    }
}
