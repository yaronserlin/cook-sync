package com.dtos.request.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data Transfer Object bundling a plain paginated endpoint's {@code page}/{@code size}
 * parameters — no sort or facet filters. Bound server-side from query parameters via
 * {@code @ModelAttribute}, and unpacked client-side into a Retrofit {@code @QueryMap} via
 * {@link #toQueryMap()}.
 *
 * @param page zero-based page index; {@code null} (an omitted query parameter) defaults to 0
 * @param size page size limit; {@code null} (an omitted query parameter) defaults to 20
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public record PageRequestDTO(
        Integer page,
        Integer size
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    /**
     * Normalizes an omitted {@code page}/{@code size} query parameter (bound as {@code null}) to
     * this request's default.
     */
    public PageRequestDTO {
        if (page == null) {
            page = DEFAULT_PAGE;
        }
        if (size == null) {
            size = DEFAULT_SIZE;
        }
    }

    /**
     * Unpacks this request into a Retrofit {@code @QueryMap}-compatible map.
     *
     * @return this request's fields, keyed by their query parameter name
     */
    public Map<String, String> toQueryMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("page", String.valueOf(page));
        map.put("size", String.valueOf(size));
        return map;
    }
}
