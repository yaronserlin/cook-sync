package com.dtos.request.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link RecipeSearchRequestDTO}'s compact-constructor defaulting and
 * {@link RecipeSearchRequestDTO#toQueryMap()} null-omission behavior.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class RecipeSearchRequestDTOTest {

    @Test
    void constructor_DefaultsPageAndSize_WhenNull() {
        RecipeSearchRequestDTO request = new RecipeSearchRequestDTO(null, null, null, null, null, null, null, null);

        assertEquals(0, request.page());
        assertEquals(20, request.size());
    }

    @Test
    void toQueryMap_OmitsNullFacetFields() {
        RecipeSearchRequestDTO request = new RecipeSearchRequestDTO(null, null, null, null, null, null, 0, 20);

        assertNull(request.toQueryMap().get("q"));
        assertNull(request.toQueryMap().get("author"));
        assertNull(request.toQueryMap().get("ingredient"));
        assertNull(request.toQueryMap().get("sortBy"));
        assertNull(request.toQueryMap().get("difficulty"));
        assertNull(request.toQueryMap().get("minRating"));
    }

    @Test
    void toQueryMap_IncludesNonNullFields() {
        RecipeSearchRequestDTO request =
                new RecipeSearchRequestDTO("pasta", "gordon", "flour", "newest", "EASY", 4.5, 1, 10);

        assertEquals("pasta", request.toQueryMap().get("q"));
        assertEquals("gordon", request.toQueryMap().get("author"));
        assertEquals("flour", request.toQueryMap().get("ingredient"));
        assertEquals("newest", request.toQueryMap().get("sortBy"));
        assertEquals("EASY", request.toQueryMap().get("difficulty"));
        assertEquals("4.5", request.toQueryMap().get("minRating"));
        assertEquals("1", request.toQueryMap().get("page"));
        assertEquals("10", request.toQueryMap().get("size"));
        assertFalse(request.toQueryMap().containsKey("null"));
    }
}
