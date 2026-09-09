package com.dtos.request.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link RecipeTagFilterRequestDTO}'s compact-constructor defaulting and
 * {@link RecipeTagFilterRequestDTO#toQueryMap()} null-omission behavior.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class RecipeTagFilterRequestDTOTest {

    @Test
    void constructor_DefaultsPageAndSize_WhenNull() {
        RecipeTagFilterRequestDTO request = new RecipeTagFilterRequestDTO(null, null, null, null, null);

        assertEquals(0, request.page());
        assertEquals(20, request.size());
    }

    @Test
    void toQueryMap_OmitsNullFacetFields() {
        RecipeTagFilterRequestDTO request = new RecipeTagFilterRequestDTO(null, null, null, 0, 20);

        assertNull(request.toQueryMap().get("sortBy"));
        assertNull(request.toQueryMap().get("difficulty"));
        assertNull(request.toQueryMap().get("minRating"));
    }

    @Test
    void toQueryMap_IncludesNonNullFields() {
        RecipeTagFilterRequestDTO request = new RecipeTagFilterRequestDTO("fastest", "MEDIUM", 2.5, 1, 5);

        assertEquals("fastest", request.toQueryMap().get("sortBy"));
        assertEquals("MEDIUM", request.toQueryMap().get("difficulty"));
        assertEquals("2.5", request.toQueryMap().get("minRating"));
        assertEquals("1", request.toQueryMap().get("page"));
        assertEquals("5", request.toQueryMap().get("size"));
    }
}
