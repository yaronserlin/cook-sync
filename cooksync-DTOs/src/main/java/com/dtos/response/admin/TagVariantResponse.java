package com.dtos.response.admin;

/**
 * Data Transfer Object representing a single tag variant within a duplicate-tag group,
 * augmented with how many recipes currently use it. Lets the admin moderation UI show which
 * variant is the "real" one before choosing a canonical tag to merge into.
 *
 * @param id unique identifier of the tag record
 * @param name display label of the tag
 * @param recipeCount number of recipes currently tagged with this variant
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public record TagVariantResponse(
        String id,
        String name,
        long recipeCount
) {
}
