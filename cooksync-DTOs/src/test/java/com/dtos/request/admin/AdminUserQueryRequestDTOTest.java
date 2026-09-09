package com.dtos.request.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link AdminUserQueryRequestDTO}'s compact-constructor defaulting and
 * {@link AdminUserQueryRequestDTO#toQueryMap()} null-omission behavior.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class AdminUserQueryRequestDTOTest {

    @Test
    void constructor_DefaultsPageAndSize_WhenNull() {
        AdminUserQueryRequestDTO request = new AdminUserQueryRequestDTO(null, null, null, null, null, null);

        assertEquals(0, request.page());
        assertEquals(20, request.size());
    }

    @Test
    void toQueryMap_OmitsNullFacetFields() {
        AdminUserQueryRequestDTO request = new AdminUserQueryRequestDTO(0, 20, null, null, null, null);

        assertNull(request.toQueryMap().get("q"));
        assertNull(request.toQueryMap().get("enabled"));
        assertNull(request.toQueryMap().get("sortBy"));
        assertNull(request.toQueryMap().get("direction"));
    }

    @Test
    void toQueryMap_IncludesNonNullFields() {
        AdminUserQueryRequestDTO request =
                new AdminUserQueryRequestDTO(1, 25, "gordon", true, "email", "asc");

        assertEquals("1", request.toQueryMap().get("page"));
        assertEquals("25", request.toQueryMap().get("size"));
        assertEquals("gordon", request.toQueryMap().get("q"));
        assertEquals("true", request.toQueryMap().get("enabled"));
        assertEquals("email", request.toQueryMap().get("sortBy"));
        assertEquals("asc", request.toQueryMap().get("direction"));
    }
}
