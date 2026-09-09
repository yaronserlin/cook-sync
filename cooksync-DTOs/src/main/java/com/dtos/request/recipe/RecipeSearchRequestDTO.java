package com.dtos.request.recipe;

import java.util.LinkedHashMap;
import java.util.Map;

import com.dtos.request.common.PaginationDefaults;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object bundling the recipe catalog search's keyword/facet, pagination, and sort
 * parameters. Bound server-side from query parameters via {@code @ModelAttribute} on
 * {@code RecipeController#searchRecipes}, and unpacked client-side into a Retrofit
 * {@code @QueryMap} via {@link #toQueryMap()} for {@code ApiService#searchRecipes}.
 *
 * @param q unified free-text search string
 * @param author author name filter string
 * @param ingredient ingredient name filter string
 * @param sortBy sort criterion: newest (default), rating, fastest
 * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
 * @param minRating optional minimum average rating threshold
 * @param page zero-based page index; {@code null} (an omitted query parameter) defaults to 0
 * @param size page size limit; {@code null} (an omitted query parameter) defaults to 20
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public record RecipeSearchRequestDTO(
        @Size(max = 200, message = "Search query must be at most 200 characters")
        String q,

        @Size(max = 200, message = "Author filter must be at most 200 characters")
        String author,

        @Size(max = 200, message = "Ingredient filter must be at most 200 characters")
        String ingredient,

        @Pattern(regexp = "newest|rating|fastest", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "sortBy must be newest, rating, or fastest")
        String sortBy,

        @Pattern(regexp = "EASY|MEDIUM|HARD", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Difficulty must be EASY, MEDIUM, or HARD")
        String difficulty,

        Double minRating,
        Integer page,
        Integer size
) {
    /**
     * Normalizes an omitted {@code page}/{@code size} query parameter (bound as {@code null}) to
     * this request's default, via {@link PaginationDefaults}.
     */
    public RecipeSearchRequestDTO {
        page = PaginationDefaults.normalizePage(page);
        size = PaginationDefaults.normalizeSize(size);
    }

    /**
     * Unpacks this request into a Retrofit {@code @QueryMap}-compatible map, omitting any field
     * left {@code null} rather than sending it as the literal string {@code "null"}.
     *
     * @return this request's non-null fields, keyed by their query parameter name
     */
    public Map<String, String> toQueryMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (q != null) {
            map.put("q", q);
        }
        if (author != null) {
            map.put("author", author);
        }
        if (ingredient != null) {
            map.put("ingredient", ingredient);
        }
        if (sortBy != null) {
            map.put("sortBy", sortBy);
        }
        if (difficulty != null) {
            map.put("difficulty", difficulty);
        }
        if (minRating != null) {
            map.put("minRating", String.valueOf(minRating));
        }
        map.put("page", String.valueOf(page));
        map.put("size", String.valueOf(size));
        return map;
    }
}
