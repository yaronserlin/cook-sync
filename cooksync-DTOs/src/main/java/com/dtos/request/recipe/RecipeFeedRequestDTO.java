package com.dtos.request.recipe;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data Transfer Object bundling the public recipe feed's pagination, sort, and facet-filter
 * parameters. Bound server-side from query parameters via {@code @ModelAttribute} on
 * {@code RecipeController#getAllRecipesPaged}, and unpacked client-side into a Retrofit
 * {@code @QueryMap} via {@link #toQueryMap()} for {@code ApiService#getPublicFeed}.
 *
 * @param page zero-based page index; {@code null} (an omitted query parameter) defaults to 0
 * @param size page size limit; {@code null} (an omitted query parameter) defaults to 20
 * @param sortBy sort criterion: newest (default), rating, fastest
 * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
 * @param minRating optional minimum average rating threshold
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public record RecipeFeedRequestDTO(
        Integer page,
        Integer size,
        String sortBy,
        String difficulty,
        Double minRating
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    /**
     * Normalizes an omitted {@code page}/{@code size} query parameter (bound as {@code null}) to
     * this request's default.
     */
    public RecipeFeedRequestDTO {
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
        map.put("page", String.valueOf(page));
        map.put("size", String.valueOf(size));
        if (sortBy != null) {
            map.put("sortBy", sortBy);
        }
        if (difficulty != null) {
            map.put("difficulty", difficulty);
        }
        if (minRating != null) {
            map.put("minRating", String.valueOf(minRating));
        }
        return map;
    }
}
