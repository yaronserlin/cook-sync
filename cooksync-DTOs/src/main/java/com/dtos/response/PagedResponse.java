package com.dtos.response;

import java.util.List;

/**
 * Generic Data Transfer Object wrapper for paginated result collections.
 * Encapsulates list content and pagination metadata for list endpoints.
 *
 * @param <T> the type of items contained within the page
 * @param content list of data items for the current page
 * @param page 0-based index of the returned page
 * @param size requested page size capacity
 * @param totalElements aggregate count of matching records across all pages
 * @param totalPages aggregate count of calculated pages
 * @param last boolean flag indicating whether current page is the final page
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
