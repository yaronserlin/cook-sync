package com.cooksync_server.services;

import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.IngredientRepository;
import com.cooksync_server.repositories.InstructionRepository;
import com.cooksync_server.repositories.RecipeImageRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.TagRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.cooksync_server.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test suite verifying recipe query, detail retrieval, and pagination in RecipeService.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/08/2026
 */
@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private InstructionRepository instructionRepository;
    @Mock
    private RecipeImageRepository recipeImageRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private ICloudinaryService cloudinaryService;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe sampleRecipe;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user-1")
                .firstName("Gordon")
                .lastName("Ramsay")
                .email("gordon@cooksync.com")
                .build();

        sampleRecipe = Recipe.builder()
                .id("recipe-100")
                .title("Classic Beef Wellington")
                .description("A delicious savory steak pastry")
                .createdBy(sampleUser)
                .difficulty(Recipe.Difficulty.HARD)
                .visibility(Recipe.Visibility.PUBLIC)
                .prepTimeMinutes(45)
                .cookTimeMinutes(40)
                .servings(4)
                .reviewCount(1)
                .averageRating(5.0)
                .build();
    }

    @Test
    void getRecipeById_ShouldReturnRecipeResponse_WhenRecipeExists() {
        when(recipeRepository.findByIdWithDetails("recipe-100")).thenReturn(Optional.of(sampleRecipe));

        RecipeResponse response = recipeService.getRecipeById("recipe-100");

        assertNotNull(response);
        assertEquals("recipe-100", response.id());
        assertEquals("Classic Beef Wellington", response.title());
        assertNotNull(response.createdBy());
        assertEquals("Gordon", response.createdBy().firstName());
        assertEquals("Ramsay", response.createdBy().lastName());
        verify(recipeRepository, times(1)).findByIdWithDetails("recipe-100");
    }

    @Test
    void getRecipeById_ShouldThrowResourceNotFoundException_WhenRecipeDoesNotExist() {
        when(recipeRepository.findByIdWithDetails("non-existent-id")).thenReturn(Optional.empty());
        when(recipeRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipeService.getRecipeById("non-existent-id"));
    }

    @Test
    void getAllRecipesPaged_ShouldReturnPagedResponse() {
        Page<Recipe> recipePage = new PageImpl<>(List.of(sampleRecipe), PageRequest.of(0, 10), 1);
        when(recipeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(recipePage);

        PagedResponse<RecipePreviewResponse> response = recipeService.getAllRecipesPaged(0, 10, "newest", null, null);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Classic Beef Wellington", response.content().get(0).title());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());
    }
}
