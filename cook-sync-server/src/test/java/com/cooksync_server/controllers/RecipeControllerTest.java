package com.cooksync_server.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cooksync_server.config.JwtUtil;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.services.RecipeService;
import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web-layer test suite verifying {@link RecipeController}'s request mapping, payload validation,
 * and status-code wiring against a mocked {@link RecipeService}. Complements the service-layer
 * {@code RecipeServiceTest} by catching controller-only regressions (wrong path/param names,
 * missing {@code @Valid}, wrong HTTP status) that a service-only test suite cannot see.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@WebMvcTest(controllers = RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecipeService recipeService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is auto-registered by
     * {@code @WebMvcTest} as a servlet {@code Filter}; mocking its {@code JwtUtil} dependency
     * just satisfies that bean's constructor (no {@code Authorization} header is sent here).
     */
    @MockitoBean
    private JwtUtil jwtUtil;

    private RecipePreviewResponse samplePreview() {
        return new RecipePreviewResponse("recipe-1", "Jane Doe", "Pasta", "Tasty pasta",
                "EASY", "PUBLIC", 10, 20, 0, null, "2026-08-01T00:00:00Z",
                List.of(), null, false, null);
    }

    private RecipeCreateRequestDTO validCreateRequest() {
        return new RecipeCreateRequestDTO(
                "Pasta", "EASY", "PUBLIC", 10, 20, 2, List.of(),
                List.of(new IngredientRequestDTO(null, "Flour", 200, "unit-1")),
                List.of(new InstructionRequestDTO(1, "Mix it", false, null, List.of(), null)),
                null, List.of());
    }

    @Test
    @WithMockUser
    void getAllRecipesPaged_ShouldReturnOk_WithPagedContent() throws Exception {
        PagedResponse<RecipePreviewResponse> paged = new PagedResponse<>(List.of(samplePreview()), 0, 20, 1, 1, true);
        when(recipeService.getAllRecipesPaged(eq(0), eq(20), isNull(), isNull(), isNull())).thenReturn(paged);

        mockMvc.perform(get("/api/recipes/paged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("recipe-1"));
    }

    @Test
    @WithMockUser
    void getRecipeById_ShouldReturnOk_WhenRecipeExists() throws Exception {
        RecipeResponse response = new RecipeResponse("recipe-1", null, "Pasta", "EASY", "PUBLIC",
                10, 20, 2, 0, null, List.of(), "2026-08-01T00:00:00Z", null, List.of(), java.util.Set.of(),
                List.of(), null, List.of());
        when(recipeService.getRecipeById("recipe-1")).thenReturn(response);

        mockMvc.perform(get("/api/recipes/recipe-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Pasta"));
    }

    @Test
    @WithMockUser
    void getRecipeById_ShouldReturnNotFound_WhenRecipeMissing() throws Exception {
        when(recipeService.getRecipeById("missing")).thenThrow(new ResourceNotFoundException("Recipe", "missing"));

        mockMvc.perform(get("/api/recipes/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void createRecipe_ShouldReturnCreated_WhenPayloadValid() throws Exception {
        RecipeCreateRequestDTO request = validCreateRequest();
        RecipeResponse response = new RecipeResponse("recipe-1", null, "Pasta", "EASY", "PUBLIC",
                10, 20, 2, 0, null, List.of(), "2026-08-01T00:00:00Z", null, List.of(), java.util.Set.of(),
                List.of(), null, List.of());
        when(recipeService.createRecipe(any(RecipeCreateRequestDTO.class), eq("chef@example.com"))).thenReturn(response);

        mockMvc.perform(post("/api/recipes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("recipe-1"));
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void createRecipe_ShouldReturnBadRequest_WhenTitleMissing() throws Exception {
        RecipeCreateRequestDTO invalidRequest = new RecipeCreateRequestDTO(
                "", "EASY", "PUBLIC", 10, 20, 2, List.of(),
                List.of(new IngredientRequestDTO(null, "Flour", 200, "unit-1")),
                List.of(new InstructionRequestDTO(1, "Mix it", false, null, List.of(), null)),
                null, List.of());

        mockMvc.perform(post("/api/recipes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void createRecipe_ShouldReturnBadRequest_WhenNoIngredients() throws Exception {
        RecipeCreateRequestDTO invalidRequest = new RecipeCreateRequestDTO(
                "Pasta", "EASY", "PUBLIC", 10, 20, 2, List.of(),
                List.of(),
                List.of(new InstructionRequestDTO(1, "Mix it", false, null, List.of(), null)),
                null, List.of());

        mockMvc.perform(post("/api/recipes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void updateVisibility_ShouldReturnBadRequest_WhenVisibilityBlank() throws Exception {
        RecipeVisibilityUpdateRequestDTO invalidRequest = new RecipeVisibilityUpdateRequestDTO("");

        mockMvc.perform(patch("/api/recipes/recipe-1/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void deleteRecipe_ShouldReturnOk_WhenOwnerDeletes() throws Exception {
        mockMvc.perform(delete("/api/recipes/recipe-1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void createRecipe_ShouldReturnForbidden_WhenServiceDeniesOwnership() throws Exception {
        RecipeCreateRequestDTO request = validCreateRequest();
        when(recipeService.createRecipe(any(RecipeCreateRequestDTO.class), eq("chef@example.com")))
                .thenThrow(new UnauthorizedActionException("You are not allowed to create this recipe."));

        mockMvc.perform(post("/api/recipes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void updateRecipe_ShouldReturnOk_WhenPayloadValid() throws Exception {
        RecipeCreateRequestDTO request = validCreateRequest();
        RecipeResponse response = new RecipeResponse("recipe-1", null, "Pasta", "EASY", "PUBLIC",
                10, 20, 2, 0, null, List.of(), "2026-08-01T00:00:00Z", null, List.of(), java.util.Set.of(),
                List.of(), null, List.of());
        when(recipeService.updateRecipe(eq("recipe-1"), any(RecipeCreateRequestDTO.class), eq("chef@example.com")))
                .thenReturn(response);

        mockMvc.perform(put("/api/recipes/recipe-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("recipe-1"));
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void updateRecipe_ShouldReturnForbidden_WhenCallerDoesNotOwnRecipe() throws Exception {
        RecipeCreateRequestDTO request = validCreateRequest();
        when(recipeService.updateRecipe(eq("recipe-1"), any(RecipeCreateRequestDTO.class), eq("chef@example.com")))
                .thenThrow(new UnauthorizedActionException("You are not allowed to edit this recipe."));

        mockMvc.perform(put("/api/recipes/recipe-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void updateRecipe_ShouldReturnNotFound_WhenRecipeMissing() throws Exception {
        RecipeCreateRequestDTO request = validCreateRequest();
        when(recipeService.updateRecipe(eq("missing"), any(RecipeCreateRequestDTO.class), eq("chef@example.com")))
                .thenThrow(new ResourceNotFoundException("Recipe", "missing"));

        mockMvc.perform(put("/api/recipes/missing")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void updateVisibility_ShouldReturnOk_WhenPayloadValid() throws Exception {
        RecipeVisibilityUpdateRequestDTO request = new RecipeVisibilityUpdateRequestDTO("PRIVATE");
        RecipeResponse response = new RecipeResponse("recipe-1", null, "Pasta", "EASY", "PRIVATE",
                10, 20, 2, 0, null, List.of(), "2026-08-01T00:00:00Z", null, List.of(), java.util.Set.of(),
                List.of(), null, List.of());
        when(recipeService.updateVisibility(eq("recipe-1"), any(RecipeVisibilityUpdateRequestDTO.class), eq("chef@example.com")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/recipes/recipe-1/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void updateVisibility_ShouldReturnForbidden_WhenCallerDoesNotOwnRecipe() throws Exception {
        RecipeVisibilityUpdateRequestDTO request = new RecipeVisibilityUpdateRequestDTO("PRIVATE");
        when(recipeService.updateVisibility(eq("recipe-1"), any(RecipeVisibilityUpdateRequestDTO.class), eq("chef@example.com")))
                .thenThrow(new UnauthorizedActionException("You are not allowed to edit this recipe."));

        mockMvc.perform(patch("/api/recipes/recipe-1/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void updateVisibility_ShouldReturnNotFound_WhenRecipeMissing() throws Exception {
        RecipeVisibilityUpdateRequestDTO request = new RecipeVisibilityUpdateRequestDTO("PRIVATE");
        when(recipeService.updateVisibility(eq("missing"), any(RecipeVisibilityUpdateRequestDTO.class), eq("chef@example.com")))
                .thenThrow(new ResourceNotFoundException("Recipe", "missing"));

        mockMvc.perform(patch("/api/recipes/missing/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void deleteRecipe_ShouldReturnForbidden_WhenCallerDoesNotOwnRecipe() throws Exception {
        org.mockito.Mockito.doThrow(new UnauthorizedActionException("You are not allowed to delete this recipe."))
                .when(recipeService).deleteRecipe("recipe-1", "chef@example.com");

        mockMvc.perform(delete("/api/recipes/recipe-1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef@example.com")
    void deleteRecipe_ShouldReturnNotFound_WhenRecipeMissing() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Recipe", "missing"))
                .when(recipeService).deleteRecipe("missing", "chef@example.com");

        mockMvc.perform(delete("/api/recipes/missing")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void searchRecipes_ShouldReturnOk_WithQueryParams() throws Exception {
        PagedResponse<RecipePreviewResponse> paged = new PagedResponse<>(List.of(samplePreview()), 0, 20, 1, 1, true);
        when(recipeService.searchRecipes(eq("pasta"), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(paged);

        mockMvc.perform(get("/api/recipes/search").param("q", "pasta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Pasta"));
    }
}
