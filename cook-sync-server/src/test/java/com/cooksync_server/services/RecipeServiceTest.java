package com.cooksync_server.services;

import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.DescriptionBlockDTO;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.IngredientRepository;
import com.cooksync_server.repositories.InstructionRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeImageRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.TagRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.cooksync_server.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit test suite verifying recipe query, detail retrieval, and pagination in RecipeServiceImp.
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
    private CloudinaryService cloudinaryService;
    @Mock
    private FavoriteRecipeRepository favoriteRecipeRepository;
    @Mock
    private PersonalInstructionNoteRepository personalInstructionNoteRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;

    @InjectMocks
    private RecipeServiceImp recipeService;

    private Recipe sampleRecipe;
    private User sampleUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user-1")
                .firstName("Gordon")
                .lastName("Ramsay")
                .email("gordon@cooksync.com")
                .build();

        otherUser = User.builder()
                .id("user-2")
                .firstName("Julia")
                .lastName("Child")
                .email("julia@cooksync.com")
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

    @Test
    void getPublicRecipesByUser_ShouldReturnRecipes_WhenUserOptedIntoPublicVisibility() {
        sampleUser.setShowRecipesPublicly(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));
        Page<Recipe> recipePage = new PageImpl<>(List.of(sampleRecipe), PageRequest.of(0, 10), 1);
        when(recipeRepository.findByCreatedByIdAndVisibility("user-1", Recipe.Visibility.PUBLIC, PageRequest.of(0, 10)))
                .thenReturn(recipePage);

        PagedResponse<RecipePreviewResponse> response = recipeService.getPublicRecipesByUser("user-1", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Classic Beef Wellington", response.content().get(0).title());
    }

    @Test
    void getPublicRecipesByUser_ShouldReturnEmptyPage_WhenUserOptedOutOfPublicVisibility() {
        sampleUser.setShowRecipesPublicly(false);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        PagedResponse<RecipePreviewResponse> response = recipeService.getPublicRecipesByUser("user-1", 0, 10);

        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        verify(recipeRepository, never()).findByCreatedByIdAndVisibility(
                any(String.class), any(Recipe.Visibility.class), any(Pageable.class));
    }

    @Test
    void getPublicRecipesByUser_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipeService.getPublicRecipesByUser("missing", 0, 10));
    }

    // ------------------------------------------------------------------
    // createRecipe
    // ------------------------------------------------------------------

    @Test
    void createRecipe_ShouldSaveRecipeWithMappedFields_WhenRequestIsValid() {
        Tag sampleTag = Tag.builder().id("tag-1").name("Dinner").build();
        Unit sampleUnit = Unit.builder().id("unit-1").code("g").name("Gram").build();
        IngredientRequestDTO ingredientDto = new IngredientRequestDTO("tmp-1", "Flour", 200, "unit-1");
        InstructionRequestDTO instructionDto = new InstructionRequestDTO(1, "Mix ingredients", false, null, List.of(), null);
        DescriptionBlockDTO blockDto = new DescriptionBlockDTO("TEXT", "A tasty dish", null, null, false);
        RecipeCreateRequestDTO request = new RecipeCreateRequestDTO(
                "New Recipe", "medium", "PUBLIC", 10, 20, 4,
                List.of("tag-1"), List.of(ingredientDto), List.of(instructionDto),
                "http://img/new.jpg", List.of(blockDto));

        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(tagRepository.findAllById(List.of("tag-1"))).thenReturn(List.of(sampleTag));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(unitRepository.findAllById(any())).thenReturn(List.of(sampleUnit));

        RecipeResponse response = recipeService.createRecipe(request, "gordon@cooksync.com");

        assertNotNull(response);
        assertEquals("New Recipe", response.title());
        assertEquals("MEDIUM", response.difficulty());
        assertEquals("PUBLIC", response.visibility());
        assertEquals(1, response.ingredients().size());
        assertEquals(1, response.tags().size());

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        Recipe savedRecipe = captor.getValue();
        assertEquals("New Recipe", savedRecipe.getTitle());
        assertEquals(Recipe.Difficulty.MEDIUM, savedRecipe.getDifficulty());
        assertEquals(sampleUser, savedRecipe.getCreatedBy());
        assertEquals(10, savedRecipe.getPrepTimeMinutes());
        assertEquals(20, savedRecipe.getCookTimeMinutes());
        assertEquals(4, savedRecipe.getServings());
    }

    @Test
    void createRecipe_ShouldThrowResourceNotFoundException_WhenUserNotFound() {
        RecipeCreateRequestDTO request = new RecipeCreateRequestDTO(
                "New Recipe", "easy", "PUBLIC", 10, 20, 4,
                List.of(), List.of(new IngredientRequestDTO(null, "Salt", 1, "unit-1")),
                List.of(new InstructionRequestDTO(1, "Mix", false, null, List.of(), null)),
                null, List.of());
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> recipeService.createRecipe(request, "missing@cooksync.com"));
    }

    @Test
    void createRecipe_ShouldThrowResourceNotFoundException_WhenTagMissing() {
        RecipeCreateRequestDTO request = new RecipeCreateRequestDTO(
                "New Recipe", "easy", "PUBLIC", 10, 20, 4,
                List.of("missing-tag"), List.of(new IngredientRequestDTO(null, "Salt", 1, "unit-1")),
                List.of(new InstructionRequestDTO(1, "Mix", false, null, List.of(), null)),
                null, List.of());
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(tagRepository.findAllById(List.of("missing-tag"))).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> recipeService.createRecipe(request, "gordon@cooksync.com"));

        verify(recipeRepository, never()).save(any(Recipe.class));
    }

    @Test
    void createRecipe_ShouldThrowResourceNotFoundException_WhenUnitMissing() {
        RecipeCreateRequestDTO request = new RecipeCreateRequestDTO(
                "New Recipe", "easy", "PUBLIC", 10, 20, 4,
                List.of(), List.of(new IngredientRequestDTO(null, "Salt", 1, "missing-unit")),
                List.of(new InstructionRequestDTO(1, "Mix", false, null, List.of(), null)),
                null, List.of());
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(unitRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> recipeService.createRecipe(request, "gordon@cooksync.com"));
    }

    // ------------------------------------------------------------------
    // updateRecipe
    // ------------------------------------------------------------------

    @Test
    void updateRecipe_ShouldUpdateFieldsAndDeleteRemovedImages_WhenUserIsOwner() {
        Recipe recipeToUpdate = Recipe.builder()
                .id("recipe-100")
                .title("Old Title")
                .description("Old description")
                .createdBy(sampleUser)
                .difficulty(Recipe.Difficulty.EASY)
                .visibility(Recipe.Visibility.PUBLIC)
                .prepTimeMinutes(10)
                .cookTimeMinutes(10)
                .servings(2)
                .build();
        recipeToUpdate.getImages().add(RecipeImage.builder()
                .recipe(recipeToUpdate).imageUrl("http://img/old-primary.jpg").isPrimary(true).build());
        recipeToUpdate.getInstructions().add(Instruction.builder()
                .id("inst-old").recipe(recipeToUpdate).stepNumber(1).description("Old step")
                .imageUrl("http://img/old-instruction.jpg").build());
        recipeToUpdate.getDescriptionBlocks().add(DescriptionBlock.builder()
                .id("block-1").recipe(recipeToUpdate).type(DescriptionBlock.BlockType.IMAGE)
                .imageUrl("http://img/old-block.jpg").sortOrder(0).build());

        Unit sampleUnit = Unit.builder().id("unit-1").code("g").name("Gram").build();
        IngredientRequestDTO newIngredientDto = new IngredientRequestDTO(null, "Salt", 5, "unit-1");
        InstructionRequestDTO newInstructionDto = new InstructionRequestDTO(1, "New step", false, null, List.of(), null);
        DescriptionBlockDTO newBlockDto = new DescriptionBlockDTO("TEXT", "New description", null, null, false);
        RecipeCreateRequestDTO updateRequest = new RecipeCreateRequestDTO(
                "Updated Title", "MEDIUM", "PUBLIC", 15, 20, 3,
                List.of(), List.of(newIngredientDto), List.of(newInstructionDto),
                "http://img/new-primary.jpg", List.of(newBlockDto));

        when(recipeRepository.findById("recipe-100")).thenReturn(Optional.of(recipeToUpdate));
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(unitRepository.findAllById(any())).thenReturn(List.of(sampleUnit));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeResponse response = recipeService.updateRecipe("recipe-100", updateRequest, "gordon@cooksync.com");

        assertNotNull(response);
        assertEquals("Updated Title", response.title());
        assertEquals("MEDIUM", response.difficulty());
        assertEquals("http://img/new-primary.jpg", response.primaryImageUrl());

        ArgumentCaptor<List> removedCaptor = ArgumentCaptor.forClass(List.class);
        verify(cloudinaryService).deleteImages(removedCaptor.capture());
        List<String> removedImageUrls = removedCaptor.getValue();
        assertEquals(3, removedImageUrls.size());
        assertTrue(removedImageUrls.contains("http://img/old-primary.jpg"));
        assertTrue(removedImageUrls.contains("http://img/old-instruction.jpg"));
        assertTrue(removedImageUrls.contains("http://img/old-block.jpg"));

        verify(recipeRepository).save(recipeToUpdate);
    }

    @Test
    void updateRecipe_ShouldThrowUnauthorizedActionException_WhenUserIsNotOwner() {
        RecipeCreateRequestDTO request = new RecipeCreateRequestDTO(
                "Updated Title", "MEDIUM", "PUBLIC", 15, 20, 3,
                List.of(), List.of(new IngredientRequestDTO(null, "Salt", 5, "unit-1")),
                List.of(new InstructionRequestDTO(1, "Step", false, null, List.of(), null)),
                null, List.of());
        when(recipeRepository.findById("recipe-100")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("julia@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> recipeService.updateRecipe("recipe-100", request, "julia@cooksync.com"));

        verify(recipeRepository, never()).save(any(Recipe.class));
        verify(cloudinaryService, never()).deleteImages(any());
    }

    // ------------------------------------------------------------------
    // deleteRecipe
    // ------------------------------------------------------------------

    @Test
    void deleteRecipe_ShouldDeleteRecipeAndCascadeCleanup_WhenUserIsOwner() {
        Recipe recipeToDelete = Recipe.builder()
                .id("recipe-100")
                .title("Beef Wellington")
                .createdBy(sampleUser)
                .build();
        recipeToDelete.getImages().add(RecipeImage.builder()
                .recipe(recipeToDelete).imageUrl("http://img/1.jpg").isPrimary(true).build());

        when(recipeRepository.findById("recipe-100")).thenReturn(Optional.of(recipeToDelete));
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(cloudinaryService.buildUserFolder(eq("gordon@cooksync.com"), eq("Beef_Wellington")))
                .thenReturn("cooksync-dev/gordon@cooksync.com/Beef_Wellington");

        recipeService.deleteRecipe("recipe-100", "gordon@cooksync.com");

        verify(reviewReportRepository).deleteByRecipeId("recipe-100");
        verify(favoriteRecipeRepository).deleteByRecipeId("recipe-100");
        verify(personalInstructionNoteRepository).deleteByRecipeId("recipe-100");
        verify(cloudinaryService).deleteImages(List.of("http://img/1.jpg"));
        verify(cloudinaryService).deleteFolder("cooksync-dev/gordon@cooksync.com/Beef_Wellington");
        verify(recipeRepository).delete(recipeToDelete);
    }

    @Test
    void deleteRecipe_ShouldThrowUnauthorizedActionException_WhenUserIsNotOwner() {
        when(recipeRepository.findById("recipe-100")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("julia@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> recipeService.deleteRecipe("recipe-100", "julia@cooksync.com"));

        verify(recipeRepository, never()).delete(any(Recipe.class));
        verify(reviewReportRepository, never()).deleteByRecipeId(any());
        verify(favoriteRecipeRepository, never()).deleteByRecipeId(any());
        verify(personalInstructionNoteRepository, never()).deleteByRecipeId(any());
        verify(cloudinaryService, never()).deleteImages(any());
        verify(cloudinaryService, never()).deleteFolder(any());
    }

    // ------------------------------------------------------------------
    // updateVisibility
    // ------------------------------------------------------------------

    @Test
    void updateVisibility_ShouldUpdateVisibility_WhenUserIsOwner() {
        RecipeVisibilityUpdateRequestDTO request = new RecipeVisibilityUpdateRequestDTO("PRIVATE");
        when(recipeRepository.findById("recipe-100")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeResponse response = recipeService.updateVisibility("recipe-100", request, "gordon@cooksync.com");

        assertNotNull(response);
        assertEquals("PRIVATE", response.visibility());
        assertEquals(Recipe.Visibility.PRIVATE, sampleRecipe.getVisibility());
        verify(recipeRepository).save(sampleRecipe);
    }

    @Test
    void updateVisibility_ShouldThrowUnauthorizedActionException_WhenUserIsNotOwner() {
        RecipeVisibilityUpdateRequestDTO request = new RecipeVisibilityUpdateRequestDTO("PRIVATE");
        when(recipeRepository.findById("recipe-100")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("julia@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> recipeService.updateVisibility("recipe-100", request, "julia@cooksync.com"));

        verify(recipeRepository, never()).save(any(Recipe.class));
    }

    // ------------------------------------------------------------------
    // searchRecipes / findRecipesByTag / getMyRecipes
    // ------------------------------------------------------------------

    @Test
    void searchRecipes_ShouldReturnMatchingRecipes_WhenKeywordMatches() {
        Page<Recipe> recipePage = new PageImpl<>(List.of(sampleRecipe), PageRequest.of(0, 10), 1);
        when(recipeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(recipePage);

        PagedResponse<RecipePreviewResponse> response =
                recipeService.searchRecipes("beef", null, null, "newest", null, null, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Classic Beef Wellington", response.content().get(0).title());
    }

    @Test
    void findRecipesByTag_ShouldReturnMatchingRecipes_WhenTagExists() {
        Page<Recipe> recipePage = new PageImpl<>(List.of(sampleRecipe), PageRequest.of(0, 10), 1);
        when(recipeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(recipePage);

        PagedResponse<RecipePreviewResponse> response =
                recipeService.findRecipesByTag("dinner", "newest", null, null, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Classic Beef Wellington", response.content().get(0).title());
    }

    @Test
    void getMyRecipes_ShouldReturnUsersRecipes_WhenUserExists() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        Page<Recipe> recipePage = new PageImpl<>(List.of(sampleRecipe), PageRequest.of(0, 10), 1);
        when(recipeRepository.findByCreatedById("user-1", PageRequest.of(0, 10))).thenReturn(recipePage);

        PagedResponse<RecipePreviewResponse> response = recipeService.getMyRecipes("gordon@cooksync.com", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Classic Beef Wellington", response.content().get(0).title());
    }

    @Test
    void getMyRecipes_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> recipeService.getMyRecipes("missing@cooksync.com", 0, 10));
    }
}
