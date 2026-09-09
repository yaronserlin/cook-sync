package com.dtos.request.common;

/**
 * Shared {@code page}/{@code size} defaults for query DTOs bound from optional query parameters.
 * Centralizes the "an omitted parameter, bound as {@code null}, defaults to this value"
 * normalization that {@link PageRequestDTO} and the other paginated request DTOs
 * ({@code RecipeSearchRequestDTO}, {@code RecipeFeedRequestDTO}, {@code RecipeTagFilterRequestDTO},
 * {@code com.dtos.request.admin.AdminUserQueryRequestDTO}) would otherwise each redeclare
 * identically in their own compact constructor — Java records can't share state via inheritance,
 * so a static helper is the next-best de-duplication.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public final class PaginationDefaults {

    /** Default zero-based page index used when a paginated request omits {@code page}. */
    public static final int DEFAULT_PAGE = 0;

    /** Default page size used when a paginated request omits {@code size}. */
    public static final int DEFAULT_SIZE = 20;

    private PaginationDefaults() {
    }

    /**
     * @param page the raw, possibly-omitted page index
     * @return {@code page}, or {@link #DEFAULT_PAGE} if it was {@code null}
     */
    public static int normalizePage(Integer page) {
        return page == null ? DEFAULT_PAGE : page;
    }

    /**
     * @param size the raw, possibly-omitted page size
     * @return {@code size}, or {@link #DEFAULT_SIZE} if it was {@code null}
     */
    public static int normalizeSize(Integer size) {
        return size == null ? DEFAULT_SIZE : size;
    }
}
