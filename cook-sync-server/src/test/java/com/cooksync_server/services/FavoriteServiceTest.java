package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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

import com.cooksync_server.entities.FavoriteRecipe;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;

/**
 * Unit test suite verifying favorite bookmark additions, removals, and paginated retrieval in FavoriteServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRecipeRepository favoriteRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonalInstructionNoteRepository personalInstructionNoteRepository;

    @InjectMocks
    private FavoriteServiceImp favoriteService;

    private User sampleUser;
    private Recipe sampleRecipe;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id("user-1").email("gordon@cooksync.com").build();
        sampleRecipe = Recipe.builder().id("recipe-1").title("Beef Wellington").createdBy(sampleUser).build();
    }

    @Test
    void addFavorite_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> favoriteService.addFavorite("recipe-1", "missing@cooksync.com"));
    }

    @Test
    void addFavorite_ShouldSaveFavorite_WhenNotAlreadyBookmarked() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(java.util.Optional.of(sampleUser));
        when(recipeRepository.findById("recipe-1")).thenReturn(java.util.Optional.of(sampleRecipe));
        when(favoriteRepository.existsByUserIdAndRecipeId("user-1", "recipe-1")).thenReturn(false);

        favoriteService.addFavorite("recipe-1", "gordon@cooksync.com");

        verify(favoriteRepository).save(org.mockito.ArgumentMatchers.any(FavoriteRecipe.class));
    }

    @Test
    void addFavorite_ShouldNotSaveDuplicate_WhenAlreadyBookmarked() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(java.util.Optional.of(sampleUser));
        when(recipeRepository.findById("recipe-1")).thenReturn(java.util.Optional.of(sampleRecipe));
        when(favoriteRepository.existsByUserIdAndRecipeId("user-1", "recipe-1")).thenReturn(true);

        favoriteService.addFavorite("recipe-1", "gordon@cooksync.com");

        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any(FavoriteRecipe.class));
    }

    @Test
    void removeFavorite_ShouldDeleteFavoriteAndAssociatedNote() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(java.util.Optional.of(sampleUser));
        when(recipeRepository.findById("recipe-1")).thenReturn(java.util.Optional.of(sampleRecipe));

        favoriteService.removeFavorite("recipe-1", "gordon@cooksync.com");

        verify(favoriteRepository).deleteByUserIdAndRecipeId("user-1", "recipe-1");
        verify(personalInstructionNoteRepository).deleteByUserIdAndRecipeId("user-1", "recipe-1");
    }

    @Test
    void getUserFavorites_ShouldReturnPagedResponseWithRecipePreviews() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(java.util.Optional.of(sampleUser));
        FavoriteRecipe favorite = FavoriteRecipe.builder().user(sampleUser).recipe(sampleRecipe).build();
        Page<FavoriteRecipe> page = new PageImpl<>(List.of(favorite), PageRequest.of(0, 10), 1);
        when(favoriteRepository.findByUserId(org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);
        when(personalInstructionNoteRepository.existsByUserIdAndRecipeId("user-1", "recipe-1")).thenReturn(false);
        when(personalInstructionNoteRepository.findByUserIdAndRecipeIdAndInstructionIdIsNull("user-1", "recipe-1"))
                .thenReturn(java.util.Optional.empty());

        PagedResponse<RecipePreviewResponse> response = favoriteService.getUserFavorites("gordon@cooksync.com", 0, 10);

        assertEquals(1, response.content().size());
        assertEquals("Beef Wellington", response.content().get(0).title());
    }
}
