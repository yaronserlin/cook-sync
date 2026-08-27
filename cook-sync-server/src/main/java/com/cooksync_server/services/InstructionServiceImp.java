package com.cooksync_server.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.response.instruction.InstructionResponse;
import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
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
     * @param recipeId target recipe ID
     * @param request instruction step creation request DTO
     * @param userEmail user email address
     * @return InstructionResponse DTO of saved step
     * @throws ResourceNotFoundException if the recipe or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the recipe owner nor an administrator
     */
    @Transactional
    public InstructionResponse addInstructionToRecipe(String recipeId, InstructionRequestDTO request, String userEmail) {
        Recipe recipe = OwnershipValidator.requireOwnedResource(
                () -> recipeRepository.findById(recipeId), EntityNames.RECIPE, recipeId,
                r -> r.getCreatedBy().getId(), userRepository, userEmail,
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
     * @param instructionId target instruction step ID
     * @param request instruction step update request DTO
     * @param userEmail user email address
     * @return InstructionResponse DTO of updated step
     * @throws ResourceNotFoundException if the instruction or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the instruction's recipe owner nor an administrator
     */
    @Transactional
    public InstructionResponse updateInstruction(String instructionId, InstructionRequestDTO request, String userEmail) {
        Instruction instruction = OwnershipValidator.requireOwnedResource(
                () -> instructionRepository.findById(instructionId), EntityNames.INSTRUCTION, instructionId,
                i -> i.getRecipe().getCreatedBy().getId(), userRepository, userEmail,
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
     * @param instructionId target instruction step ID
     * @param userEmail user email address
     * @throws ResourceNotFoundException if the instruction or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the instruction's recipe owner nor an administrator
     */
    @Transactional
    public void deleteInstruction(String instructionId, String userEmail) {
        Instruction instruction = OwnershipValidator.requireOwnedResource(
                () -> instructionRepository.findById(instructionId), EntityNames.INSTRUCTION, instructionId,
                i -> i.getRecipe().getCreatedBy().getId(), userRepository, userEmail,
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
