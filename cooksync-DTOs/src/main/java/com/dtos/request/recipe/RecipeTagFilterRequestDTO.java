package com.dtos.request.recipe;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data Transfer Object bundling the tag-filtered recipe browse's pagination, sort, and
 * facet-filter parameters (the target tag name itself travels as a path variable, not a field
 * here). Bound server-side from query parameters via {@code @ModelAttribute} on
 * {@code RecipeController#getRecipesByTag}, and unpacked client-side into a Retrofit
 * {@code @QueryMap} via {@link #toQueryMap()} for {@code ApiService#getRecipesByTag}.
 *
 * @param sortBy sort criterion: newest (default), rating, fastest
 * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
 * @param minRating optional minimum average rating threshold
 * @param page zero-based page index; {@code null} (an omitted query parameter) defaults to 0
 * @param size page size limit; {@code null} (an omitted query parameter) defaults to 20
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public record RecipeTagFilterRequestDTO(
        String sortBy,
        String difficulty,
        Double minRating,
        Integer page,
        Integer size
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    /**
     * Normalizes an omitted {@code page}/{@code size} query parameter (bound as {@code null}) to
     * this request's default.
     */
    public RecipeTagFilterRequestDTO {
        if (page == null) {
            page = DEFAULT_PAGE;
        }
        if (size == null) {
            size = DEFAULT_SIZE;
        }
    }

    /**
     * Unpacks this request into a Retrofit {@code @QueryMap}-compatible map, omitting any field
     * left {@code null} rather than sending it as the literal string {@code "null"}.
     *
     * @return this request's non-null fields, keyed by their query parameter name
     */
    public Map<String, String> toQueryMap() {
        Map<String, String> map = new LinkedHashMap<>();
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
