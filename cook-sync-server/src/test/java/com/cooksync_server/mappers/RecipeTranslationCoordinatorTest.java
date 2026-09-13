package com.cooksync_server.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cooksync_server.entities.ContentTranslation.EntityType;
import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.services.TranslationService;
import com.cooksync_server.services.TranslationService.TranslatedText;
import com.cooksync_server.translation.TranslationAttempt;

/**
 * Unit test suite for {@link RecipeTranslationCoordinator}: the entity-traversal specifics of
 * building one recipe's {@link RecipeTranslationBundle}. Publishes a mocked
 * {@link TranslationService} through the package-private {@link TranslationAccess} bridge (this
 * test lives in the same package) so every field's resolution outcome is fully controlled without
 * bootstrapping a Spring context.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
class RecipeTranslationCoordinatorTest {

    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        translationService = mock(TranslationService.class);
        new TranslationAccess(translationService).publish();
    }

    @AfterEach
    void tearDown() {
        // Reset the static bridge so this mock never leaks into another test class sharing the
        // same JVM (e.g. RecipeServiceTest, which relies on the no-Spring-context fallback).
        new TranslationAccess(null).publish();
    }

    @Test
    void resolveForDetail_translatesEveryField_whenAllSucceed() {
        stubEveryFieldAsTranslated();

        RecipeTranslationBundle bundle = RecipeTranslationCoordinator.resolveForDetail(detailRecipe());

        assertEquals("[Shakshuka]", bundle.valueOf(EntityType.RECIPE_TITLE, "recipe-1", "Shakshuka"));
        assertTrue(bundle.isMachineTranslated());
    }

    @Test
    void resolveForDetail_fallsBackEveryFieldToOriginal_whenOneFieldFails() {
        when(translationService.resolve(any(), anyString(), anyString(), anyString(), any(TranslationAttempt.class)))
                .thenAnswer(inv -> {
                    String original = inv.getArgument(2);
                    if ("Flour".equals(original)) {
                        return new TranslatedText("Flour", false, true);
                    }
                    return new TranslatedText("[" + original + "]", true, false);
                });

        RecipeTranslationBundle bundle = RecipeTranslationCoordinator.resolveForDetail(detailRecipe());

        assertEquals("Shakshuka", bundle.valueOf(EntityType.RECIPE_TITLE, "recipe-1", "Shakshuka"),
                "the title succeeded on its own but must still fall back since the ingredient failed");
        assertFalse(bundle.isMachineTranslated());
    }

    @Test
    void resolveForDetail_resolvesASharedUnit_exactlyOnce_regardlessOfIngredientCount() {
        stubEveryFieldAsTranslated();
        Unit sharedUnit = Unit.builder().id("unit-1").code("g").name("Gram").namePlural("Grams").build();
        Ingredient flour = Ingredient.builder().id("ing-1").name("Flour").unit(sharedUnit).build();
        Ingredient sugar = Ingredient.builder().id("ing-2").name("Sugar").unit(sharedUnit).build();
        Recipe recipe = Recipe.builder()
                .id("recipe-1")
                .title("Cake")
                .description("A rich chocolate cake")
                .sourceLocale("en")
                .ingredients(new LinkedHashSet<>(Set.of(flour, sugar)))
                .build();

        RecipeTranslationCoordinator.resolveForDetail(recipe);

        verify(translationService, times(1)).resolve(
                eq(EntityType.UNIT_NAME), eq("unit-1"), eq("Gram"), eq("en"), any(TranslationAttempt.class));
        verify(translationService, times(1)).resolve(
                eq(EntityType.UNIT_NAME_PLURAL), eq("unit-1"), eq("Grams"), eq("en"), any(TranslationAttempt.class));
    }

    @Test
    void resolveForDetail_resolvesFlatDescription_whenNoDescriptionBlocksArePersisted() {
        stubEveryFieldAsTranslated();
        Recipe recipe = Recipe.builder()
                .id("recipe-1")
                .title("Shakshuka")
                .description("A savory egg dish")
                .sourceLocale("en")
                .build();

        RecipeTranslationCoordinator.resolveForDetail(recipe);

        verify(translationService).resolve(eq(EntityType.RECIPE_DESCRIPTION), eq("recipe-1"),
                eq("A savory egg dish"), eq("en"), any(TranslationAttempt.class));
        verify(translationService, never()).resolve(eq(EntityType.RECIPE_DESCRIPTION_BLOCK), anyString(),
                anyString(), anyString(), any(TranslationAttempt.class));
    }

    @Test
    void resolveForDetail_resolvesDescriptionBlocks_insteadOfFlatDescription_whenBlocksArePersisted() {
        stubEveryFieldAsTranslated();
        DescriptionBlock textBlock = DescriptionBlock.builder()
                .id("block-1").type(DescriptionBlock.BlockType.TEXT).text("A savory egg dish").build();
        List<DescriptionBlock> blocks = new ArrayList<>(List.of(textBlock));
        Recipe recipe = Recipe.builder()
                .id("recipe-1")
                .title("Shakshuka")
                .description("legacy flat description should be ignored")
                .sourceLocale("en")
                .descriptionBlocks(blocks)
                .build();

        RecipeTranslationCoordinator.resolveForDetail(recipe);

        verify(translationService).resolve(eq(EntityType.RECIPE_DESCRIPTION_BLOCK), eq("block-1"),
                eq("A savory egg dish"), eq("en"), any(TranslationAttempt.class));
        verify(translationService, never()).resolve(eq(EntityType.RECIPE_DESCRIPTION), anyString(),
                anyString(), anyString(), any(TranslationAttempt.class));
    }

    @Test
    void resolveForPreview_onlyResolvesTitleAndDescription() {
        stubEveryFieldAsTranslated();

        RecipeTranslationCoordinator.resolveForPreview(detailRecipe());

        verify(translationService, never()).resolve(eq(EntityType.INGREDIENT_NAME), anyString(),
                anyString(), anyString(), any(TranslationAttempt.class));
        verify(translationService, never()).resolve(eq(EntityType.TAG_NAME), anyString(),
                anyString(), anyString(), any(TranslationAttempt.class));
    }

    private void stubEveryFieldAsTranslated() {
        when(translationService.resolve(any(), anyString(), anyString(), anyString(), any(TranslationAttempt.class)))
                .thenAnswer(inv -> new TranslatedText("[" + inv.getArgument(2) + "]", true, false));
    }

    private static Recipe detailRecipe() {
        Unit unit = Unit.builder().id("unit-1").code("g").name("Gram").namePlural("Grams").build();
        Ingredient ingredient = Ingredient.builder().id("ing-1").name("Flour").unit(unit).build();
        Instruction instruction = Instruction.builder().id("inst-1").stepNumber(1).description("Mix well").build();
        Tag tag = Tag.builder().id("tag-1").name("Breakfast").build();
        return Recipe.builder()
                .id("recipe-1")
                .title("Shakshuka")
                .description("A savory egg dish")
                .sourceLocale("en")
                .ingredients(new LinkedHashSet<>(Set.of(ingredient)))
                .instructions(new LinkedHashSet<>(Set.of(instruction)))
                .tags(new LinkedHashSet<>(Set.of(tag)))
                .build();
    }
}
