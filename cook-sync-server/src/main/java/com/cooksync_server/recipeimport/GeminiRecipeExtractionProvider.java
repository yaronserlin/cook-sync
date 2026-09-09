package com.cooksync_server.recipeimport;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link RecipeExtractionProvider} backed by the Gemini API (Google AI Studio) — free tier, a
 * single API key, no billing account required.
 *
 * <p>Deliberately does <b>not</b> use Gemini's {@code responseSchema} constrained-decoding
 * feature: empirically (live testing against the real API while building this), a
 * schema-constrained request to the current flash model reliably failed with a transient
 * "high demand" 503, while the identical prompt with only {@code responseMimeType:
 * "application/json"} — the exact target JSON shape spelled out in the prompt text instead —
 * succeeded reliably and extracted every field (including correctly grounding each ingredient's
 * unit against the app's real unit codes) on the first try. This class parses that shape
 * defensively regardless (unrecognized fields ignored, a malformed response yields {@link
 * Optional#empty()} rather than throwing), so a future Gemini-side fix to the schema feature
 * would not require any caller-visible change.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@Slf4j
@Component
public class GeminiRecipeExtractionProvider implements RecipeExtractionProvider {

    private static final int TIMEOUT_MS = 45_000;
    private static final String MODEL = "gemini-flash-latest";

    /** Tolerant of any field Gemini's response happens to include beyond what we read. */
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final RestClient client = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .requestFactory(timeoutRequestFactory())
            .build();

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    @Override
    public Optional<ExtractedRecipe> extractFromWebPage(String pageText, List<String> unitCodes) {
        if (!StringUtils.hasText(pageText)) {
            return Optional.empty();
        }
        Map<String, Object> textPart = Map.of("text", buildPrompt(pageText, unitCodes));
        return callGemini(List.of(textPart));
    }

    @Override
    public Optional<ExtractedRecipe> extractFromImages(List<ImageInput> images, List<String> unitCodes) {
        if (images == null || images.isEmpty()) {
            return Optional.empty();
        }
        String instruction = images.size() == 1
                ? "The recipe is shown in the attached photo (a cookbook page, printed page, or handwritten note). "
                        + "Read it and extract the same fields."
                : "The recipe is shown across the " + images.size() + " attached photos (e.g. successive pages, or an "
                        + "ingredients list and method on separate cards) — read them together as one recipe and extract the same fields.";
        Map<String, Object> textPart = Map.of("text", buildPrompt(instruction, unitCodes));

        List<Map<String, Object>> parts = new java.util.ArrayList<>();
        parts.add(textPart);
        for (ImageInput image : images) {
            parts.add(Map.of("inline_data", Map.of(
                    "mime_type", StringUtils.hasText(image.mimeType()) ? image.mimeType() : "image/jpeg",
                    "data", Base64.getEncoder().encodeToString(image.bytes()))));
        }
        return callGemini(parts);
    }

    /**
     * Builds the extraction instruction text shared by both entry points: the exact JSON shape
     * and field names to respond with, the unit-code vocabulary to ground ingredient units in
     * (the app's real, current {@code units.code} values — not a hardcoded list, so a unit added
     * later via the admin console is picked up automatically), and the source content itself.
     *
     * @param sourceContent the page text (or a short image-description sentence for photo imports)
     * @param unitCodes the real unit codes to constrain {@code unitCode} choices to
     * @return the full prompt text
     */
    private String buildPrompt(String sourceContent, List<String> unitCodes) {
        return """
                Extract structured recipe data from the recipe content below. Respond with ONLY a JSON \
                object using EXACTLY these keys (camelCase, no others, no markdown code fences):
                {"title": string, "description": string (short intro, empty string if none), \
                "difficulty": "EASY"|"MEDIUM"|"HARD", "prepTimeMinutes": integer, "cookTimeMinutes": integer, \
                "servings": integer, "ingredients": [{"name": string, "quantity": number, "unitCode": string}], \
                "instructions": [string]}

                Rules:
                - Convert all time expressions to total minutes (e.g. "1 hour 20 minutes" -> 80).
                - unitCode must be EXACTLY one of: %s. If nothing fits (e.g. a countable item like "2 eggs"), use "piece".
                - instructions is a plain array of step description strings, in order, one string per step.
                - If a field is not mentioned, use a reasonable default (0 for times, 4 for servings, "EASY" for difficulty).
                - If the content is not actually a recipe, respond with {"title": ""} and nothing else.

                RECIPE CONTENT:

                %s
                """.formatted(String.join(", ", unitCodes), sourceContent);
    }

    /**
     * Sends one {@code generateContent} request and parses the result into an {@link
     * ExtractedRecipe}. Distinguishes two very different kinds of failure: the call to Gemini
     * itself not working (network error, non-2xx response — e.g. the free tier's daily quota
     * exhausted, or a transient "high demand" 503 — or a malformed response envelope) throws
     * {@link ExtractionServiceException}, since that says nothing about whether the given content
     * is actually a recipe; the model responding fine but the content genuinely not being a
     * recipe (an empty title, per the prompt's own convention, or malformed model-output JSON) is
     * treated as {@link Optional#empty()}, exactly like {@code TranslationService} falls back
     * silently on a translation miss. {@code RecipeImportServiceImp} maps the two to distinct job
     * error codes so the user sees an honest reason rather than always "couldn't find a recipe."
     *
     * @param parts the request's content parts (text, optionally plus an inline image)
     * @return the extracted recipe, or empty if the content wasn't a recipe
     * @throws ExtractionServiceException if the call to Gemini itself failed
     */
    private Optional<ExtractedRecipe> callGemini(List<Map<String, Object>> parts) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("GEMINI_API_KEY is not set; skipping recipe extraction");
            throw new ExtractionServiceException("GEMINI_API_KEY is not set", null);
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of("responseMimeType", "application/json"));

        String extractedJson;
        try {
            String rawResponse = client.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", MODEL, apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            extractedJson = extractResponseText(rawResponse);
        } catch (RuntimeException | java.io.IOException e) {
            log.warn("Gemini recipe extraction service call failed: {}", e.getMessage());
            throw new ExtractionServiceException("Gemini call failed", e);
        }

        if (extractedJson == null) {
            log.warn("Gemini response had no usable candidate text");
            return Optional.empty();
        }

        return parseExtractedRecipe(extractedJson);
    }

    /**
     * Pulls the model's text out of Gemini's response envelope.
     *
     * @param rawResponse the raw JSON response body
     * @return the first candidate's text, or {@code null} if the response has no usable candidate
     * @throws com.fasterxml.jackson.core.JsonProcessingException if the envelope itself isn't valid JSON
     */
    static String extractResponseText(String rawResponse) throws com.fasterxml.jackson.core.JsonProcessingException {
        GeminiResponse response = JSON.readValue(rawResponse, GeminiResponse.class);
        if (response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        GeminiResponse.Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return null;
        }
        return candidate.content().parts().get(0).text();
    }

    /**
     * Parses the model's JSON-shaped text (per {@link #buildPrompt}'s dictated field names) into
     * an {@link ExtractedRecipe}.
     *
     * @param extractedJson the model's raw JSON text output
     * @return the parsed recipe, or empty if it's malformed or the model signaled "not a recipe"
     *         (a blank title, per the prompt's explicit instruction)
     */
    static Optional<ExtractedRecipe> parseExtractedRecipe(String extractedJson) {
        try {
            ExtractedRecipeJson parsed = JSON.readValue(extractedJson, ExtractedRecipeJson.class);
            if (!StringUtils.hasText(parsed.title())) {
                return Optional.empty();
            }
            List<ExtractedRecipe.ExtractedIngredient> ingredients = parsed.ingredients() == null
                    ? List.of()
                    : parsed.ingredients().stream()
                        .map(i -> new ExtractedRecipe.ExtractedIngredient(i.name(), i.quantity(), i.unitCode()))
                        .toList();
            List<String> instructions = parsed.instructions() == null ? List.of() : parsed.instructions();

            return Optional.of(new ExtractedRecipe(
                    parsed.title(),
                    parsed.description() == null ? "" : parsed.description(),
                    StringUtils.hasText(parsed.difficulty()) ? parsed.difficulty() : "EASY",
                    Math.max(parsed.prepTimeMinutes(), 0),
                    Math.max(parsed.cookTimeMinutes(), 0),
                    Math.max(parsed.servings(), 1),
                    ingredients,
                    instructions));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Failed to parse Gemini's extracted recipe JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return factory;
    }

    /** Shape of Gemini's {@code generateContent} response, for the fields this class reads. */
    private record GeminiResponse(List<Candidate> candidates) {
        record Candidate(Content content) {
        }

        record Content(List<Part> parts) {
        }

        record Part(String text) {
        }
    }

    /** Shape of the model's own JSON text output, matching {@link #buildPrompt}'s dictated keys. */
    private record ExtractedRecipeJson(
            String title,
            String description,
            String difficulty,
            int prepTimeMinutes,
            int cookTimeMinutes,
            int servings,
            List<ExtractedIngredientJson> ingredients,
            List<String> instructions) {

        record ExtractedIngredientJson(
                String name,
                @JsonProperty("quantity") BigDecimal quantity,
                String unitCode) {
        }
    }
}
