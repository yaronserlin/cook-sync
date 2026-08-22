package com.dtos.response.admin;

import java.util.List;

/**
 * Data Transfer Object grouping potential duplicate tags identified during administrative audit.
 * Holds the common normalized tag key alongside its matching variations.
 *
 * @param normalizedName the normalized tag string representation
 * @param variants list of matching tag variants (with per-variant recipe usage counts) identified for consolidation
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public record DuplicateTagGroupResponse(
        String normalizedName,
        List<TagVariantResponse> variants
) {
}
