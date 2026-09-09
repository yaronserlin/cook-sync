package com.cooksync_server.recipeimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link GeminiRecipeExtractionProvider}'s pure response-parsing logic:
 * pulling the model's text out of Gemini's response envelope, and parsing that text into an
 * {@link ExtractedRecipe}. Does not exercise {@link GeminiRecipeExtractionProvider#extractFromWebPage}
 * or {@code extractFromImage} themselves, which make a real HTTP call.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
class GeminiRecipeExtractionProviderTest {

    @Test
    void extractResponseText_returnsCandidateText_forWellFormedResponse() throws Exception {
        String raw = """
                {"candidates":[{"content":{"parts":[{"text":"{\\"title\\":\\"Test\\"}"}]}}]}
                """;

        String text = GeminiRecipeExtractionProvider.extractResponseText(raw);

        assertEquals("{\"title\":\"Test\"}", text);
    }

    @Test
    void extractResponseText_returnsNull_whenNoCandidates() throws Exception {
        String raw = "{\"candidates\":[]}";

        assertNull(GeminiRecipeExtractionProvider.extractResponseText(raw));
    }

    @Test
    void extractResponseText_returnsNull_whenPartsEmpty() throws Exception {
        String raw = "{\"candidates\":[{\"content\":{\"parts\":[]}}]}";

        assertNull(GeminiRecipeExtractionProvider.extractResponseText(raw));
    }

    @Test
    void extractResponseText_toleratesUnknownFields() throws Exception {
        // Real Gemini responses carry many extra fields (usageMetadata, modelVersion,
        // thoughtSignature, ...) this class doesn't model - they must not break parsing.
        String raw = """
                {"candidates":[{"content":{"parts":[{"text":"ok","thoughtSignature":"xyz"}],"role":"model"},"finishReason":"STOP","index":0}],
                 "usageMetadata":{"promptTokenCount":1},"modelVersion":"gemini-3.6-flash","responseId":"abc"}
                """;

        assertEquals("ok", GeminiRecipeExtractionProvider.extractResponseText(raw));
    }

    @Test
    void parseExtractedRecipe_parsesFullyPopulatedJson() {
        String json = """
                {"title":"Roast Chicken","description":"A classic.","difficulty":"EASY",
                 "prepTimeMinutes":15,"cookTimeMinutes":80,"servings":4,
                 "ingredients":[{"name":"chicken","quantity":1.8,"unitCode":"kg"},
                                {"name":"lemons","quantity":2,"unitCode":"piece"}],
                 "instructions":["Preheat the oven.","Roast the chicken."]}
                """;

        Optional<ExtractedRecipe> result = GeminiRecipeExtractionProvider.parseExtractedRecipe(json);

        assertTrue(result.isPresent());
        ExtractedRecipe recipe = result.get();
        assertEquals("Roast Chicken", recipe.title());
        assertEquals("A classic.", recipe.description());
        assertEquals("EASY", recipe.difficulty());
        assertEquals(15, recipe.prepTimeMinutes());
        assertEquals(80, recipe.cookTimeMinutes());
        assertEquals(4, recipe.servings());
        assertEquals(2, recipe.ingredients().size());
        assertEquals("kg", recipe.ingredients().get(0).unitCode());
        assertEquals(0, recipe.ingredients().get(0).quantity().compareTo(new BigDecimal("1.8")));
        assertEquals(2, recipe.instructions().size());
    }

    @Test
    void parseExtractedRecipe_returnsEmpty_whenTitleBlank() {
        // The extraction prompt explicitly instructs the model to respond this way for
        // non-recipe content.
        String json = "{\"title\":\"\"}";

        assertTrue(GeminiRecipeExtractionProvider.parseExtractedRecipe(json).isEmpty());
    }

    @Test
    void parseExtractedRecipe_returnsEmpty_forMalformedJson() {
        assertTrue(GeminiRecipeExtractionProvider.parseExtractedRecipe("not json at all").isEmpty());
    }

    @Test
    void parseExtractedRecipe_defaultsMissingOptionalFields() {
        String json = "{\"title\":\"Minimal Recipe\"}";

        Optional<ExtractedRecipe> result = GeminiRecipeExtractionProvider.parseExtractedRecipe(json);

        assertTrue(result.isPresent());
        ExtractedRecipe recipe = result.get();
        assertEquals("", recipe.description());
        assertEquals("EASY", recipe.difficulty());
        assertEquals(0, recipe.prepTimeMinutes());
        assertEquals(0, recipe.cookTimeMinutes());
        assertEquals(1, recipe.servings());
        assertFalse(recipe.ingredients() == null);
        assertTrue(recipe.ingredients().isEmpty());
        assertTrue(recipe.instructions().isEmpty());
    }

    @Test
    void parseExtractedRecipe_clampsNegativeTimes_toZero() {
        String json = "{\"title\":\"Weird Recipe\",\"prepTimeMinutes\":-5,\"cookTimeMinutes\":-1,\"servings\":0}";

        ExtractedRecipe recipe = GeminiRecipeExtractionProvider.parseExtractedRecipe(json).orElseThrow();

        assertEquals(0, recipe.prepTimeMinutes());
        assertEquals(0, recipe.cookTimeMinutes());
        assertEquals(1, recipe.servings());
    }
}
