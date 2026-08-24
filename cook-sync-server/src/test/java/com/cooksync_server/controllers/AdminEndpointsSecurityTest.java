package com.cooksync_server.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cooksync_server.config.JwtUtil;
import com.cooksync_server.services.AdminService;
import com.cooksync_server.services.UnitService;
import com.dtos.request.unit.UnitRequestDTO;
import com.dtos.response.admin.AdminStatsResponse;
import com.dtos.response.unit.UnitResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web-layer test suite verifying that {@code @PreAuthorize("hasRole('ADMIN')")}-protected
 * endpoints on {@link AdminController} and {@link UnitController} actually reject non-admin
 * callers and accept admin callers. Loads only the MVC/method-security slice (no database, no
 * JWT filter chain) — {@link MethodSecurityTestConfig} enables {@code @PreAuthorize} processing
 * so {@code @WithMockUser}'s roles are actually evaluated.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 12/08/2026
 */
@WebMvcTest(controllers = {AdminController.class, UnitController.class})
@Import(AdminEndpointsSecurityTest.MethodSecurityTestConfig.class)
class AdminEndpointsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private UnitService unitService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is a servlet {@code Filter}, so
     * {@code @WebMvcTest} auto-registers it even though {@link com.cooksync_server.config.SecurityConfig}
     * is not imported here; mocking its {@code JwtUtil} dependency just satisfies that bean's
     * constructor. Requests in this suite carry no {@code Authorization} header, so the real
     * filter logic short-circuits to a no-op pass-through without ever touching this mock.
     */
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(roles = "USER")
    void adminStats_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminStats_ShouldReturnOk_ForAdminUser() throws Exception {
        when(adminService.getStats()).thenReturn(new AdminStatsResponse(0, 0, 0, 0, 0));

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createUnit_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        UnitRequestDTO request = new UnitRequestDTO("Gram", "g");

        mockMvc.perform(post("/api/units")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUnit_ShouldReturnCreated_ForAdminUser() throws Exception {
        UnitRequestDTO request = new UnitRequestDTO("Gram", "g");
        when(unitService.createUnit(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new UnitResponse("unit-1", "g", "Gram", null, null));

        mockMvc.perform(post("/api/units")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
