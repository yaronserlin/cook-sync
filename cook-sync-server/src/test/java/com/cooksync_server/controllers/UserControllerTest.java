package com.cooksync_server.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.services.FavoriteService;
import com.cooksync_server.services.RecipeService;
import com.cooksync_server.services.UserProfileService;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.user.PublicUserProfileResponse;

import java.util.List;

/**
 * Web-layer test suite verifying {@link UserController}'s request mapping and status-code wiring
 * against mocked {@link UserProfileService}, {@link RecipeService}, and {@link FavoriteService}
 * instances.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
@WebMvcTest(controllers = UserController.class)
@WithMockUser(username = "john@example.com")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private FavoriteService favoriteService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is auto-registered by
     * {@code @WebMvcTest} as a servlet {@code Filter}; mocking its {@code JwtUtil} dependency
     * just satisfies that bean's constructor (no {@code Authorization} header is sent here).
     */
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getUserProfile_ShouldReturnPublicProfile_ExcludingSensitiveFields() throws Exception {
        PublicUserProfileResponse response = new PublicUserProfileResponse(
                "user-2", "Jane", "Smith", null, "Tel Aviv", "Home cook.", true, false);
        when(userProfileService.getUserProfileById("user-2")).thenReturn(response);

        mockMvc.perform(get("/api/users/user-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("user-2"))
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.isAdmin").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist());
    }

    @Test
    void getUserProfile_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        when(userProfileService.getUserProfileById("missing"))
                .thenThrow(new ResourceNotFoundException("User", "missing"));

        mockMvc.perform(get("/api/users/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserPublicRecipes_ShouldRouteToRecipeService_WithGivenPaging() throws Exception {
        RecipePreviewResponse recipe = new RecipePreviewResponse("recipe-1", "Jane", "Soup",
                "desc", "EASY", "PUBLIC", 10, 20, 0, null, "2026-01-01", List.of(), null, false, null);
        PagedResponse<RecipePreviewResponse> page = new PagedResponse<>(List.of(recipe), 0, 5, 1, 1, true);
        when(recipeService.getPublicRecipesByUser("user-2", 0, 5)).thenReturn(page);

        mockMvc.perform(get("/api/users/user-2/recipes").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Soup"));
    }

    @Test
    void getUserPublicFavorites_ShouldRouteToFavoriteService_WithGivenPaging() throws Exception {
        PagedResponse<RecipePreviewResponse> emptyPage = new PagedResponse<>(List.of(), 0, 20, 0, 0, true);
        when(favoriteService.getPublicFavoritesByUser(eq("user-2"), any(Integer.class), any(Integer.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/users/user-2/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }
}
