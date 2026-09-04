package com.cooksync_server.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.cooksync_server.services.AnnouncementService;
import com.cooksync_server.services.AppConfigService;
import com.cooksync_server.services.UnitService;
import com.dtos.request.announcement.AnnouncementCreateRequestDTO;
import com.dtos.request.appconfig.AppConfigUpdateRequestDTO;
import com.dtos.request.tags.TagMergeRequestDTO;
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
    private AnnouncementService announcementService;

    @MockitoBean
    private AppConfigService appConfigService;

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

    @Test
    @WithMockUser(roles = "USER")
    void deleteUnit_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(delete("/api/units/unit-1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getReportedReviews_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/admin/reviews/reported"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void dismissReport_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/admin/reviews/review-1/dismiss").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void suspendUser_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(patch("/api/admin/users/user-1/suspend").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void enableUser_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(patch("/api/admin/users/user-1/enable").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(delete("/api/admin/users/user-1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getDuplicateTagGroups_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/admin/tags/duplicates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void mergeTags_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        TagMergeRequestDTO request = new TagMergeRequestDTO("tag-1", "tag-2");

        mockMvc.perform(post("/api/admin/tags/merge")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAnnouncement_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        AnnouncementCreateRequestDTO request = new AnnouncementCreateRequestDTO("Title", "Body", "INFO");

        mockMvc.perform(post("/api/admin/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAnnouncements_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/admin/announcements"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deactivateAnnouncement_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        mockMvc.perform(patch("/api/admin/announcements/announcement-1/deactivate").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateAppConfig_ShouldReturnForbidden_ForNonAdminUser() throws Exception {
        AppConfigUpdateRequestDTO request = new AppConfigUpdateRequestDTO("ANDROID", 2, "https://example.com/app.apk");

        mockMvc.perform(put("/api/admin/app-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
