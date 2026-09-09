package com.dtos.request.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link RecipeFeedRequestDTO}'s compact-constructor defaulting and
 * {@link RecipeFeedRequestDTO#toQueryMap()} null-omission behavior.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class RecipeFeedRequestDTOTest {

    @Test
    void constructor_DefaultsPageAndSize_WhenNull() {
        RecipeFeedRequestDTO request = new RecipeFeedRequestDTO(null, null, null, null, null);

        assertEquals(0, request.page());
        assertEquals(20, request.size());
    }

    @Test
    void toQueryMap_OmitsNullFacetFields() {
        RecipeFeedRequestDTO request = new RecipeFeedRequestDTO(0, 20, null, null, null);

        assertNull(request.toQueryMap().get("sortBy"));
        assertNull(request.toQueryMap().get("difficulty"));
        assertNull(request.toQueryMap().get("minRating"));
    }

    @Test
    void toQueryMap_IncludesNonNullFields() {
        RecipeFeedRequestDTO request = new RecipeFeedRequestDTO(2, 15, "rating", "HARD", 3.0);

        assertEquals("2", request.toQueryMap().get("page"));
        assertEquals("15", request.toQueryMap().get("size"));
        assertEquals("rating", request.toQueryMap().get("sortBy"));
        assertEquals("HARD", request.toQueryMap().get("difficulty"));
        assertEquals("3.0", request.toQueryMap().get("minRating"));
    }
}
