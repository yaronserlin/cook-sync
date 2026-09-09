package com.dtos.response.recipeimport;

/**
 * Data Transfer Object representing a recipe-import job's current status.
 *
 * @param id the job's unique identifier
 * @param status one of {@code PENDING}, {@code PROCESSING}, {@code SUCCEEDED}, {@code FAILED}
 * @param errorCode a stable, machine-readable failure reason (e.g. {@code "NO_RECIPE_FOUND_PAGE"}),
 *                  set only when {@code status} is {@code FAILED} — English-only and not meant for
 *                  direct display, exactly like {@code ApiErrorResponse.errorCode}; the client
 *                  resolves it to a localized string rather than showing it verbatim
 * @param resultRecipeId the id of the (private) recipe created from this import, set only when
 *                       {@code status} is {@code SUCCEEDED} — the client fetches and opens this
 *                       recipe in the edit wizard for review
 * @param createdAt ISO formatted creation timestamp string
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public record RecipeImportJobResponse(
        String id,
        String status,
        String errorCode,
        String resultRecipeId,
        String createdAt
) {
}
