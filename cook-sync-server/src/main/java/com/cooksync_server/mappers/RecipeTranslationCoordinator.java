package com.cooksync_server.mappers;

import java.util.HashMap;
import java.util.Map;

import com.cooksync_server.entities.ContentTranslation.EntityType;
import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.services.TranslationService.TranslatedText;
import com.cooksync_server.translation.TranslationAttempt;

/**
 * Resolves every translatable field on a {@link Recipe} as one coordinated attempt, so the recipe
 * is presented as either fully translated or fully original — never a mix of languages within the
 * same recipe. Each call here creates its own fresh {@link TranslationAttempt}, so one recipe's
 * (or one preview card's) chunk failures never affect any other recipe's, card's, or request's
 * translation attempt.
 *
 * <p>See {@link RecipeTranslationBundle} for how the resulting all-or-nothing verdict is read
 * back by the mapper classes.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
final class RecipeTranslationCoordinator {

    private RecipeTranslationCoordinator() {
    }

    /**
     * Resolves every field shown on the recipe-detail screen: title, description (structured
     * blocks, or the legacy flat description when no blocks are persisted), every ingredient
     * name, every distinct unit referenced by those ingredients, every instruction's text, and
     * every tag name.
     *
     * @param recipe the recipe to resolve translations for
     * @return the resolved bundle, ready for the detail mappers to read via
     *         {@link RecipeTranslationBundle#valueOf}
     */
    static RecipeTranslationBundle resolveForDetail(Recipe recipe) {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        TranslationAttempt attempt = TranslationAttempt.create();
        String sourceLocale = recipe.getSourceLocale();

        resolveInto(bundle, attempt, EntityType.RECIPE_TITLE, recipe.getId(), recipe.getTitle(), sourceLocale);

        if (recipe.getDescriptionBlocks() != null && !recipe.getDescriptionBlocks().isEmpty()) {
            for (DescriptionBlock block : recipe.getDescriptionBlocks()) {
                if (block.getType() == DescriptionBlock.BlockType.TEXT) {
                    resolveInto(bundle, attempt, EntityType.RECIPE_DESCRIPTION_BLOCK, block.getId(), block.getText(), sourceLocale);
                }
            }
        } else {
            resolveInto(bundle, attempt, EntityType.RECIPE_DESCRIPTION, recipe.getId(), recipe.getDescription(), sourceLocale);
        }

        // Deduped by id (not by entity equality/hashCode) so two ingredients sharing the same
        // unit only translate it once per bundle.
        Map<String, Unit> distinctUnits = new HashMap<>();
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                resolveInto(bundle, attempt, EntityType.INGREDIENT_NAME, ingredient.getId(), ingredient.getName(), sourceLocale);
                if (ingredient.getUnit() != null) {
                    distinctUnits.put(ingredient.getUnit().getId(), ingredient.getUnit());
                }
            }
        }
        for (Unit unit : distinctUnits.values()) {
            resolveInto(bundle, attempt, EntityType.UNIT_NAME, unit.getId(), unit.getName(), "en");
            resolveInto(bundle, attempt, EntityType.UNIT_NAME_PLURAL, unit.getId(), unit.getNamePlural(), "en");
        }

        if (recipe.getInstructions() != null) {
            for (Instruction instruction : recipe.getInstructions()) {
                resolveInto(bundle, attempt, EntityType.INSTRUCTION_TEXT, instruction.getId(), instruction.getDescription(), sourceLocale);
            }
        }

        if (recipe.getTags() != null) {
            for (Tag tag : recipe.getTags()) {
                resolveInto(bundle, attempt, EntityType.TAG_NAME, tag.getId(), tag.getName(), "en");
            }
        }

        return bundle;
    }

    /**
     * Resolves the smaller set of fields shown on a recipe preview card: title and description
     * only. Scoped independently of {@link #resolveForDetail} so resolving the full field set for
     * every card on a list page (multiplying provider calls by ingredient/instruction/tag count
     * per card) is avoided.
     *
     * @param recipe the recipe to resolve a preview translation bundle for
     * @return the resolved bundle
     */
    static RecipeTranslationBundle resolveForPreview(Recipe recipe) {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        TranslationAttempt attempt = TranslationAttempt.create();
        String sourceLocale = recipe.getSourceLocale();

        resolveInto(bundle, attempt, EntityType.RECIPE_TITLE, recipe.getId(), recipe.getTitle(), sourceLocale);
        resolveInto(bundle, attempt, EntityType.RECIPE_DESCRIPTION, recipe.getId(), recipe.getDescription(), sourceLocale);

        return bundle;
    }

    private static void resolveInto(RecipeTranslationBundle bundle, TranslationAttempt attempt,
                                     EntityType type, String entityId, String original, String sourceLocale) {
        TranslatedText resolved = TranslationAccess.resolve(type, entityId, original, sourceLocale, attempt);
        bundle.record(type, entityId, resolved);
    }
}
