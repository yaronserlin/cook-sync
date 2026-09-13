package com.cooksync_server.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cooksync_server.entities.ContentTranslation.EntityType;
import com.cooksync_server.services.TranslationService.TranslatedText;

/**
 * Unit test suite for {@link RecipeTranslationBundle}: the all-or-nothing verdict a recipe's
 * coordinated translation attempt resolves to, and how {@link RecipeTranslationBundle#valueOf}
 * and {@link RecipeTranslationBundle#isMachineTranslated} read it back.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
class RecipeTranslationBundleTest {

    @Test
    void valueOf_returnsTranslatedValue_whenEveryFieldCompleted() {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        bundle.record(EntityType.RECIPE_TITLE, "recipe-1", new TranslatedText("שקשוקה", true, false));
        bundle.record(EntityType.INGREDIENT_NAME, "ing-1", new TranslatedText("קמח", true, false));

        assertEquals("שקשוקה", bundle.valueOf(EntityType.RECIPE_TITLE, "recipe-1", "Shakshuka"));
        assertEquals("קמח", bundle.valueOf(EntityType.INGREDIENT_NAME, "ing-1", "Flour"));
    }

    @Test
    void valueOf_returnsOriginalForEveryField_whenOneFieldFellBack() {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        bundle.record(EntityType.RECIPE_TITLE, "recipe-1", new TranslatedText("שקשוקה", true, false));
        bundle.record(EntityType.INGREDIENT_NAME, "ing-1", new TranslatedText("Flour", false, true));

        assertEquals("Shakshuka", bundle.valueOf(EntityType.RECIPE_TITLE, "recipe-1", "Shakshuka"),
                "even the field that succeeded must fall back once any field in the bundle failed");
        assertEquals("Flour", bundle.valueOf(EntityType.INGREDIENT_NAME, "ing-1", "Flour"));
    }

    @Test
    void valueOf_returnsOriginalFallback_forAFieldNeverRecordedInTheBundle() {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        bundle.record(EntityType.RECIPE_TITLE, "recipe-1", new TranslatedText("שקשוקה", true, false));

        assertEquals("Untracked", bundle.valueOf(EntityType.TAG_NAME, "tag-9", "Untracked"));
    }

    @Test
    void isMachineTranslated_isTrue_whenEveryFieldCompleted_andAtLeastOneWasMachineTranslated() {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        bundle.record(EntityType.RECIPE_TITLE, "recipe-1", new TranslatedText("שקשוקה", true, false));

        assertTrue(bundle.isMachineTranslated());
    }

    @Test
    void isMachineTranslated_isFalse_whenNoFieldNeededTranslation() {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        // A field whose request locale already matched its source locale: resolved, not a
        // failure, but not machine-translated either.
        bundle.record(EntityType.RECIPE_TITLE, "recipe-1", new TranslatedText("Shakshuka", false, false));

        assertFalse(bundle.isMachineTranslated());
    }

    @Test
    void isMachineTranslated_isFalse_whenAnyFieldFellBack_evenIfOthersWereMachineTranslated() {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        bundle.record(EntityType.RECIPE_TITLE, "recipe-1", new TranslatedText("שקשוקה", true, false));
        bundle.record(EntityType.INGREDIENT_NAME, "ing-1", new TranslatedText("Flour", false, true));

        assertFalse(bundle.isMachineTranslated());
    }

    @Test
    void aFieldThatNeededNoTranslation_doesNotByItselfBlockTheBundleFromSucceeding() {
        RecipeTranslationBundle bundle = new RecipeTranslationBundle();
        // Locale already matched source for this field: fellBack is false, not true.
        bundle.record(EntityType.TAG_NAME, "tag-1", new TranslatedText("Dinner", false, false));
        bundle.record(EntityType.RECIPE_TITLE, "recipe-1", new TranslatedText("שקשוקה", true, false));

        assertEquals("שקשוקה", bundle.valueOf(EntityType.RECIPE_TITLE, "recipe-1", "Shakshuka"));
        assertTrue(bundle.isMachineTranslated());
    }
}
