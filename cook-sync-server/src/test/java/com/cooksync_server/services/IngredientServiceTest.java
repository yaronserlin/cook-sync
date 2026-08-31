package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.IngredientRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.response.ingredient.IngredientResponse;

/**
 * Unit test suite verifying ingredient creation, update, and deletion authorization in IngredientServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IngredientServiceImp ingredientService;

    private User owner;
    private User otherUser;
    private Recipe sampleRecipe;
    private Unit sampleUnit;

    @BeforeEach
    void setUp() {
        owner = User.builder().id("user-1").email("owner@cooksync.com").build();
        otherUser = User.builder().id("user-2").email("other@cooksync.com").build();
        sampleRecipe = Recipe.builder().id("recipe-1").title("Beef Wellington").createdBy(owner).build();
        sampleUnit = Unit.builder().id("unit-1").code("g").name("Gram").build();
    }

    @Test
    void addIngredientToRecipe_ShouldSaveIngredient_WhenUserIsOwner() {
        IngredientRequestDTO request = new IngredientRequestDTO("tmp-1", "Flour", 200, "unit-1");
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("owner@cooksync.com")).thenReturn(Optional.of(owner));
        when(unitRepository.findById("unit-1")).thenReturn(Optional.of(sampleUnit));
        when(ingredientRepository.save(org.mockito.ArgumentMatchers.any(Ingredient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IngredientResponse response = ingredientService.addIngredientToRecipe("recipe-1", request, "owner@cooksync.com");

        assertEquals("Flour", response.name());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(response.quantity()));
    }

    @Test
    void addIngredientToRecipe_ShouldThrowUnauthorizedActionException_WhenUserIsNotOwner() {
        IngredientRequestDTO request = new IngredientRequestDTO("tmp-1", "Flour", 200, "unit-1");
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> ingredientService.addIngredientToRecipe("recipe-1", request, "other@cooksync.com"));
    }

    @Test
    void addIngredientToRecipe_ShouldThrowResourceNotFoundException_WhenUnitMissing() {
        IngredientRequestDTO request = new IngredientRequestDTO("tmp-1", "Flour", 200, "missing-unit");
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("owner@cooksync.com")).thenReturn(Optional.of(owner));
        when(unitRepository.findById("missing-unit")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ingredientService.addIngredientToRecipe("recipe-1", request, "owner@cooksync.com"));
    }

    @Test
    void updateIngredient_ShouldUpdateIngredient_WhenUserIsOwner() {
        Ingredient ingredient = Ingredient.builder().id("ing-1").recipe(sampleRecipe).name("Flour")
                .quantity(BigDecimal.TEN).unit(sampleUnit).build();
        IngredientRequestDTO request = new IngredientRequestDTO("tmp-1", "Sugar", 100, "unit-1");
        when(ingredientRepository.findById("ing-1")).thenReturn(Optional.of(ingredient));
        when(userRepository.findByEmail("owner@cooksync.com")).thenReturn(Optional.of(owner));
        when(unitRepository.findById("unit-1")).thenReturn(Optional.of(sampleUnit));
        when(ingredientRepository.save(org.mockito.ArgumentMatchers.any(Ingredient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IngredientResponse response = ingredientService.updateIngredient("ing-1", request, "owner@cooksync.com");

        assertEquals("Sugar", response.name());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(response.quantity()));
    }

    @Test
    void updateIngredient_ShouldThrowResourceNotFoundException_WhenIngredientMissing() {
        IngredientRequestDTO request = new IngredientRequestDTO("tmp-1", "Sugar", 100, "unit-1");
        when(ingredientRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ingredientService.updateIngredient("missing", request, "owner@cooksync.com"));
    }

    @Test
    void updateIngredient_ShouldThrowUnauthorizedActionException_WhenUserIsNotOwner() {
        Ingredient ingredient = Ingredient.builder().id("ing-1").recipe(sampleRecipe).name("Flour")
                .quantity(BigDecimal.TEN).unit(sampleUnit).build();
        IngredientRequestDTO request = new IngredientRequestDTO("tmp-1", "Sugar", 100, "unit-1");
        when(ingredientRepository.findById("ing-1")).thenReturn(Optional.of(ingredient));
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> ingredientService.updateIngredient("ing-1", request, "other@cooksync.com"));
    }

    @Test
    void updateIngredient_ShouldThrowResourceNotFoundException_WhenUnitMissing() {
        Ingredient ingredient = Ingredient.builder().id("ing-1").recipe(sampleRecipe).name("Flour")
                .quantity(BigDecimal.TEN).unit(sampleUnit).build();
        IngredientRequestDTO request = new IngredientRequestDTO("tmp-1", "Sugar", 100, "missing-unit");
        when(ingredientRepository.findById("ing-1")).thenReturn(Optional.of(ingredient));
        when(userRepository.findByEmail("owner@cooksync.com")).thenReturn(Optional.of(owner));
        when(unitRepository.findById("missing-unit")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ingredientService.updateIngredient("ing-1", request, "owner@cooksync.com"));
    }

    @Test
    void deleteIngredient_ShouldThrowResourceNotFoundException_WhenIngredientMissing() {
        when(ingredientRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ingredientService.deleteIngredient("missing", "owner@cooksync.com"));
    }

    @Test
    void deleteIngredient_ShouldDelete_WhenUserIsOwner() {
        Ingredient ingredient = Ingredient.builder().id("ing-1").recipe(sampleRecipe).name("Flour")
                .quantity(BigDecimal.TEN).unit(sampleUnit).build();
        when(ingredientRepository.findById("ing-1")).thenReturn(Optional.of(ingredient));
        when(userRepository.findByEmail("owner@cooksync.com")).thenReturn(Optional.of(owner));

        ingredientService.deleteIngredient("ing-1", "owner@cooksync.com");

        org.mockito.Mockito.verify(ingredientRepository).delete(ingredient);
    }
}
