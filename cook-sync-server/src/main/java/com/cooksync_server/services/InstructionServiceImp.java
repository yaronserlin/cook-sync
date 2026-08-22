package com.cooksync_server.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.response.instruction.InstructionResponse;
import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.mappers.InstructionMapper;
import com.cooksync_server.repositories.IngredientRepository;
import com.cooksync_server.repositories.InstructionRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class managing recipe preparation instruction steps and associated ingredient links.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class InstructionServiceImp implements InstructionService {

    private final InstructionRepository instructionRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;

    /**
     * Appends a new cooking instruction step to a recipe.
     *
     * Complexity:
     * Time: O(I) where I is count of referenced step ingredient IDs
     * Space: O(I)
     *
     * @param recipeId target recipe ID
     * @param request instruction step creation request DTO
     * @param userEmail user email address
     * @return InstructionResponse DTO of saved step
     */
    @Transactional
    public InstructionResponse addInstructionToRecipe(String recipeId, InstructionRequestDTO request, String userEmail) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", recipeId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        OwnershipValidator.requireOwnerOrAdmin(recipe.getCreatedBy().getId(), user,
                "You are not allowed to modify this recipe.");

        Instruction instruction = Instruction.builder()
                .recipe(recipe)
                .stepNumber(request.stepNumber())
                .description(request.description())
                .hasTimer(request.hasTimer())
                .timeSeconds(request.timeSeconds())
                .imageUrl(request.imageUrl())
                .ingredients(resolveIngredients(request.ingredientIds()))
                .build();

        return InstructionMapper.toResponse(instructionRepository.save(instruction));
    }

    /**
     * Updates an existing instruction step details and ingredient associations.
     *
     * Complexity:
     * Time: O(I) where I is count of referenced step ingredient IDs
     * Space: O(I)
     *
     * @param instructionId target instruction step ID
     * @param request instruction step update request DTO
     * @param userEmail user email address
     * @return InstructionResponse DTO of updated step
     */
    @Transactional
    public InstructionResponse updateInstruction(String instructionId, InstructionRequestDTO request, String userEmail) {
        Instruction instruction = instructionRepository.findById(instructionId)
                .orElseThrow(() -> new ResourceNotFoundException("Instruction", instructionId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        OwnershipValidator.requireOwnerOrAdmin(instruction.getRecipe().getCreatedBy().getId(), user,
                "You are not allowed to modify this instruction.");

        instruction.setStepNumber(request.stepNumber());
        instruction.setDescription(request.description());
        instruction.setHasTimer(request.hasTimer());
        instruction.setTimeSeconds(request.timeSeconds());
        instruction.setImageUrl(request.imageUrl());
        instruction.setIngredients(resolveIngredients(request.ingredientIds()));

        return InstructionMapper.toResponse(instructionRepository.save(instruction));
    }

    /**
     * Deletes an instruction step from a recipe.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param instructionId target instruction step ID
     * @param userEmail user email address
     */
    @Transactional
    public void deleteInstruction(String instructionId, String userEmail) {
        Instruction instruction = instructionRepository.findById(instructionId)
                .orElseThrow(() -> new ResourceNotFoundException("Instruction", instructionId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        OwnershipValidator.requireOwnerOrAdmin(instruction.getRecipe().getCreatedBy().getId(), user,
                "You are not allowed to delete this instruction.");

        instructionRepository.delete(instruction);
    }

    private Set<Ingredient> resolveIngredients(List<UUID> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return new HashSet<>();
        }
        List<String> ids = ingredientIds.stream().map(UUID::toString).collect(Collectors.toList());
        return new HashSet<>(ingredientRepository.findAllById(ids));
    }
}
