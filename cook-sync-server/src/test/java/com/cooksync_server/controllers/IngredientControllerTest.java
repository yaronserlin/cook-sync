package com.cooksync_server.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.services.IngredientService;
import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.response.ingredient.IngredientResponse;
import com.dtos.response.unit.UnitResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * Web-layer test suite verifying {@link IngredientController}'s request mapping and status-code
 * wiring against a mocked {@link IngredientService}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
@WebMvcTest(controllers = IngredientController.class)
@WithMockUser(username = "chef@example.com")
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngredientService ingredientService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is auto-registered by
     * {@code @WebMvcTest} as a servlet {@code Filter}; mocking its {@code JwtUtil} dependency
     * just satisfies that bean's constructor (no {@code Authorization} header is sent here).
     */
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void addIngredient_ShouldReturnCreated_WhenPayloadValid() throws Exception {
        IngredientRequestDTO request = new IngredientRequestDTO(null, "Flour", 2.5, "unit-1");
        IngredientResponse response = new IngredientResponse("ing-1", "Flour", BigDecimal.valueOf(2.5),
                "recipe-1", new UnitResponse("unit-1", "cup", "Cup", "Cups", null, null));
        when(ingredientService.addIngredientToRecipe(eq("recipe-1"), any(), eq("chef@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/recipes/recipe-1/ingredients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Flour"));
    }

    @Test
    void addIngredient_ShouldReturnBadRequest_WhenQuantityNotPositive() throws Exception {
        IngredientRequestDTO invalidRequest = new IngredientRequestDTO(null, "Flour", 0, "unit-1");

        mockMvc.perform(post("/api/recipes/recipe-1/ingredients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateIngredient_ShouldReturnForbidden_WhenCallerDoesNotOwnRecipe() throws Exception {
        IngredientRequestDTO request = new IngredientRequestDTO(null, "Sugar", 1.0, "unit-1");
        when(ingredientService.updateIngredient(eq("ing-1"), any(), eq("chef@example.com")))
                .thenThrow(new UnauthorizedActionException("You are not allowed to modify this ingredient."));

        mockMvc.perform(put("/api/ingredients/ing-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteIngredient_ShouldReturnOk_AndRouteCallerEmail() throws Exception {
        mockMvc.perform(delete("/api/ingredients/ing-1").with(csrf()))
                .andExpect(status().isOk());

        verify(ingredientService).deleteIngredient("ing-1", "chef@example.com");
    }

    @Test
    void addIngredient_ShouldReturnForbidden_WhenCallerDoesNotOwnRecipe() throws Exception {
        IngredientRequestDTO request = new IngredientRequestDTO(null, "Flour", 2.5, "unit-1");
        when(ingredientService.addIngredientToRecipe(eq("recipe-1"), any(), eq("chef@example.com")))
                .thenThrow(new UnauthorizedActionException("You are not allowed to modify this recipe."));

        mockMvc.perform(post("/api/recipes/recipe-1/ingredients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteIngredient_ShouldReturnForbidden_WhenCallerDoesNotOwnRecipe() throws Exception {
        doThrow(new UnauthorizedActionException("You are not allowed to modify this ingredient."))
                .when(ingredientService).deleteIngredient("ing-1", "chef@example.com");

        mockMvc.perform(delete("/api/ingredients/ing-1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteIngredient_ShouldReturnNotFound_WhenIngredientMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Ingredient", "missing"))
                .when(ingredientService).deleteIngredient("missing", "chef@example.com");

        mockMvc.perform(delete("/api/ingredients/missing").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
