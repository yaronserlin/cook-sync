package com.cooksync_server.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cooksync_server.config.JwtUtil;
import com.cooksync_server.exceptions.RateLimitExceededException;
import com.cooksync_server.services.RecipeImportService;
import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.recipeimport.RecipeImportJobResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web-layer test suite verifying {@link RecipeImportController}'s request mapping and
 * status-code wiring against a mocked {@link RecipeImportService}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@WebMvcTest(controllers = RecipeImportController.class)
@WithMockUser(username = "chef@example.com")
class RecipeImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecipeImportService recipeImportService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is auto-registered by
     * {@code @WebMvcTest} as a servlet {@code Filter}; mocking its {@code JwtUtil} dependency
     * just satisfies that bean's constructor (no {@code Authorization} header is sent here).
     */
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void startImport_ShouldReturnCreated_ForValidRequest() throws Exception {
        RecipeImportStartRequestDTO request = new RecipeImportStartRequestDTO("WEB", "https://example.com/recipe", null);
        RecipeImportJobResponse response = new RecipeImportJobResponse("job-1", "PENDING", null, null, "2026-01-01T00:00:00");
        when(recipeImportService.startImport(any(RecipeImportStartRequestDTO.class), eq("chef@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/recipe-imports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("job-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void startImport_ShouldReturnTooManyRequests_WhenRateLimited() throws Exception {
        RecipeImportStartRequestDTO request = new RecipeImportStartRequestDTO("WEB", "https://example.com/recipe", null);
        when(recipeImportService.startImport(any(RecipeImportStartRequestDTO.class), eq("chef@example.com")))
                .thenThrow(new RateLimitExceededException("Too many imports"));

        mockMvc.perform(post("/api/recipe-imports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void startImport_ShouldReturnBadRequest_WhenSourceTypeInvalid() throws Exception {
        RecipeImportStartRequestDTO request = new RecipeImportStartRequestDTO("CARRIER_PIGEON", "https://example.com/recipe", null);

        mockMvc.perform(post("/api/recipe-imports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJobStatus_ShouldReturnJobStatus() throws Exception {
        RecipeImportJobResponse response = new RecipeImportJobResponse("job-1", "SUCCEEDED", null, "recipe-1", "2026-01-01T00:00:00");
        when(recipeImportService.getJobStatus(eq("job-1"), eq("chef@example.com"))).thenReturn(response);

        mockMvc.perform(get("/api/recipe-imports/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.resultRecipeId").value("recipe-1"));
    }
}
