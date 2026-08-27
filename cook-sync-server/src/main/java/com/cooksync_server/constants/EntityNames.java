package com.cooksync_server.constants;

/**
 * Centralizes the resource-name labels passed as the first argument to
 * {@link com.cooksync_server.exceptions.ResourceNotFoundException#ResourceNotFoundException(String, String)},
 * either directly or via {@code OwnershipValidator.requireOwnedResource(...)}, so the same
 * entity is always reported under the same label.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class EntityNames {

    private EntityNames() {
    }

    public static final String USER = "User";
    public static final String RECIPE = "Recipe";
    public static final String UNIT = "Unit";
    public static final String TAG = "Tag";
    public static final String REVIEW = "Review";
    public static final String INSTRUCTION = "Instruction";
    public static final String NOTE = "Note";

    /**
     * Not part of the originally-specified constant set, but added for consistency: several
     * {@code OwnershipValidator.requireOwnedResource(...)} call sites (e.g. in
     * {@code IngredientServiceImp}) pass the literal {@code "Ingredient"} as the resource-name
     * label feeding into {@code ResourceNotFoundException}, the exact same pattern the other
     * constants above centralize.
     */
    public static final String INGREDIENT = "Ingredient";
}
