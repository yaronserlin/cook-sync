package com.cooksync_server.constants;

/**
 * Centralizes default pagination and sorting values shared across controller
 * {@code @RequestParam(defaultValue = ...)} declarations. Values are held as {@link String}
 * since that is what {@code defaultValue} requires.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 27/08/2026
 */
public final class PaginationDefaults {

    private PaginationDefaults() {
    }

    /** Default zero-based page index used when a paginated endpoint's caller omits it. */
    public static final String DEFAULT_PAGE = "0";

    /** Default page size used by every paginated endpoint, including admin listings. */
    public static final String DEFAULT_PAGE_SIZE = "20";

    /** Default maximum number of popular tags returned when the caller omits a limit. */
    public static final String POPULAR_TAGS_LIMIT = "5";

    /** Default field recipes/users are sorted by when the caller specifies none. */
    public static final String DEFAULT_SORT_FIELD = "createdAt";

    /** Default sort direction applied when the caller specifies none. */
    public static final String DEFAULT_SORT_DIRECTION = "desc";
}
