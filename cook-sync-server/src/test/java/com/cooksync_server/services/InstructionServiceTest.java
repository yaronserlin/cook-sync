package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.IngredientRepository;
import com.cooksync_server.repositories.InstructionRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.response.instruction.InstructionResponse;

/**
 * Unit test suite verifying instruction step creation, update, and deletion authorization in InstructionServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class InstructionServiceTest {

    @Mock
    private InstructionRepository instructionRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private InstructionServiceImp instructionService;

    private User owner;
    private User otherUser;
    private Recipe sampleRecipe;

    @BeforeEach
    void setUp() {
        owner = User.builder().id("user-1").email("owner@cooksync.com").build();
        otherUser = User.builder().id("user-2").email("other@cooksync.com").build();
        sampleRecipe = Recipe.builder().id("recipe-1").title("Beef Wellington").createdBy(owner).build();
    }

    @Test
    void addInstructionToRecipe_ShouldSaveStep_WhenUserIsOwner() {
        InstructionRequestDTO request = new InstructionRequestDTO(1, "Preheat the oven", false, null, List.of(), null);
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("owner@cooksync.com")).thenReturn(Optional.of(owner));
        when(instructionRepository.save(org.mockito.ArgumentMatchers.any(Instruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstructionResponse response = instructionService.addInstructionToRecipe("recipe-1", request, "owner@cooksync.com");

        assertEquals("Preheat the oven", response.description());
        assertEquals(1, response.stepNumber());
    }

    @Test
    void addInstructionToRecipe_ShouldThrowUnauthorizedActionException_WhenUserIsNotOwner() {
        InstructionRequestDTO request = new InstructionRequestDTO(1, "Preheat the oven", false, null, List.of(), null);
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(sampleRecipe));
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> instructionService.addInstructionToRecipe("recipe-1", request, "other@cooksync.com"));
    }

    @Test
    void updateInstruction_ShouldThrowResourceNotFoundException_WhenInstructionMissing() {
        InstructionRequestDTO request = new InstructionRequestDTO(1, "Preheat the oven", false, null, List.of(), null);
        when(instructionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> instructionService.updateInstruction("missing", request, "owner@cooksync.com"));
    }

    @Test
    void deleteInstruction_ShouldDelete_WhenUserIsOwner() {
        Instruction instruction = Instruction.builder().id("inst-1").recipe(sampleRecipe).stepNumber(1)
                .description("Preheat the oven").build();
        when(instructionRepository.findById("inst-1")).thenReturn(Optional.of(instruction));
        when(userRepository.findByEmail("owner@cooksync.com")).thenReturn(Optional.of(owner));

        instructionService.deleteInstruction("inst-1", "owner@cooksync.com");

        org.mockito.Mockito.verify(instructionRepository).delete(instruction);
    }

    @Test
    void deleteInstruction_ShouldThrowUnauthorizedActionException_WhenUserIsNotOwner() {
        Instruction instruction = Instruction.builder().id("inst-1").recipe(sampleRecipe).stepNumber(1)
                .description("Preheat the oven").build();
        when(instructionRepository.findById("inst-1")).thenReturn(Optional.of(instruction));
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> instructionService.deleteInstruction("inst-1", "other@cooksync.com"));
    }
}
