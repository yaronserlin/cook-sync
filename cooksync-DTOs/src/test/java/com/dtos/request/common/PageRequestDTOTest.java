package com.dtos.request.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link PageRequestDTO}'s compact-constructor defaulting and
 * {@link PageRequestDTO#toQueryMap()}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class PageRequestDTOTest {

    @Test
    void constructor_DefaultsPageAndSize_WhenNull() {
        PageRequestDTO request = new PageRequestDTO(null, null);

        assertEquals(0, request.page());
        assertEquals(20, request.size());
    }

    @Test
    void constructor_KeepsGivenPageAndSize_WhenNonNull() {
        PageRequestDTO request = new PageRequestDTO(2, 50);

        assertEquals(2, request.page());
        assertEquals(50, request.size());
    }

    @Test
    void toQueryMap_IncludesPageAndSize() {
        PageRequestDTO request = new PageRequestDTO(3, 10);

        assertEquals("3", request.toQueryMap().get("page"));
        assertEquals("10", request.toQueryMap().get("size"));
    }
}
