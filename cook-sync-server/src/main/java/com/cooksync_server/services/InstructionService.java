package com.cooksync_server.services;

import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.response.instruction.InstructionResponse;

/**
 * Service interface for managing recipe preparation instruction steps and their ingredient links.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface InstructionService {

    /**
     * Appends a new cooking instruction step to a recipe following ownership authorization.
     *
     * @param recipeId target recipe ID
     * @param request instruction step creation request DTO
     * @param userEmail authenticated user email address
     * @return InstructionResponse DTO of the saved step
     */
    InstructionResponse addInstructionToRecipe(String recipeId, InstructionRequestDTO request, String userEmail);

    /**
     * Updates an existing instruction step's details and ingredient associations.
     *
     * @param instructionId target instruction step ID
     * @param request instruction step update request DTO
     * @param userEmail authenticated user email address
     * @return InstructionResponse DTO of the updated step
     */
    InstructionResponse updateInstruction(String instructionId, InstructionRequestDTO request, String userEmail);

    /**
     * Deletes an instruction step from a recipe following ownership authorization.
     *
     * @param instructionId target instruction step ID
     * @param userEmail authenticated user email address
     */
    void deleteInstruction(String instructionId, String userEmail);
}
