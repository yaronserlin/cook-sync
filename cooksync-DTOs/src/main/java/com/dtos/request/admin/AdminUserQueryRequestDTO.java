package com.dtos.request.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import com.dtos.request.common.PaginationDefaults;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

        @Size(max = 200, message = "Search query must be at most 200 characters")
        String q,

        Boolean enabled,

        @Pattern(regexp = "firstName|lastName|email|createdAt",
                message = "sortBy must be firstName, lastName, email, or createdAt")
        String sortBy,

        @Pattern(regexp = "asc|desc", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "direction must be asc or desc")
        String direction
) {
    /**
     * Normalizes an omitted {@code page}/{@code size} query parameter (bound as {@code null}) to
     * this request's default, via {@link PaginationDefaults}.
     */
    public AdminUserQueryRequestDTO {
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
