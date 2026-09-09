package com.dtos.request.admin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data Transfer Object bundling the Admin Console user directory's pagination, search, filter,
 * and sort parameters. Bound server-side from query parameters via {@code @ModelAttribute} on
 * {@code AdminController#getAllUsers}, and unpacked client-side into a Retrofit
 * {@code @QueryMap} via {@link #toQueryMap()} for {@code ApiService#getAdminUsers}.
 *
 * @param page zero-based page index; {@code null} (an omitted query parameter) defaults to 0
 * @param size page size limit; {@code null} (an omitted query parameter) defaults to 20
 * @param q optional search fragment matched against first name, last name, or email
 * @param enabled optional account status filter (true = active, false = disabled, null = both)
 * @param sortBy field to sort by; must be one of firstName, lastName, email, createdAt
 * @param direction sort direction, "asc" or "desc"
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public record AdminUserQueryRequestDTO(
        Integer page,
        Integer size,
        String q,
        Boolean enabled,
        String sortBy,
        String direction
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    /**
     * Normalizes an omitted {@code page}/{@code size} query parameter (bound as {@code null}) to
     * this request's default.
     */
    public AdminUserQueryRequestDTO {
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
        if (q != null) {
            map.put("q", q);
        }
        if (enabled != null) {
            map.put("enabled", String.valueOf(enabled));
        }
        if (sortBy != null) {
            map.put("sortBy", sortBy);
        }
        if (direction != null) {
            map.put("direction", direction);
        }
        return map;
    }
}
