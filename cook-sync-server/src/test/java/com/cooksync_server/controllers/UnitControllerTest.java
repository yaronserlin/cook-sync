package com.cooksync_server.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cooksync_server.config.JwtUtil;
import com.cooksync_server.services.UnitService;
import com.dtos.response.PagedResponse;
import com.dtos.response.unit.UnitResponse;

import java.util.List;

/**
 * Web-layer test suite verifying {@link UnitController}'s request mapping and status-code wiring
 * against a mocked {@link UnitService}. Create/delete authorization is covered separately by
 * {@link AdminEndpointsSecurityTest}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
@WebMvcTest(controllers = UnitController.class)
@WithMockUser
class UnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnitService unitService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is auto-registered by
     * {@code @WebMvcTest} as a servlet {@code Filter}; mocking its {@code JwtUtil} dependency
     * just satisfies that bean's constructor (no {@code Authorization} header is sent here).
     */
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getAllUnits_ShouldReturnPagedUnits() throws Exception {
        UnitResponse unit = new UnitResponse("unit-1", "g", "Gram", "Grams", null, null);
        when(unitService.getAllUnits(any(Integer.class), any(Integer.class)))
                .thenReturn(new PagedResponse<>(List.of(unit), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].code").value("g"))
                .andExpect(jsonPath("$.data.content[0].name").value("Gram"))
                .andExpect(jsonPath("$.data.content[0].namePlural").value("Grams"));
    }
}
