package com.cooksync_server.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.services.InstructionService;
import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.instruction.InstructionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller providing granular management endpoints for recipe instruction steps.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InstructionController {

    private final InstructionService instructionService;

    /**
     * Adds a new preparation instruction step to a recipe.
     *
     * @param recipeId target recipe ID
     * @param request instruction creation request DTO
     * @param authentication active user authentication token
     * @return response entity containing created InstructionResponse DTO
     */
    @PostMapping("/recipes/{recipeId}/instructions")
    public ResponseEntity<ApiResponse<InstructionResponse>> addInstruction(
            @PathVariable String recipeId,
            @Valid @RequestBody InstructionRequestDTO request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        InstructionResponse response = instructionService.addInstructionToRecipe(recipeId, request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Instruction added successfully"));
    }

    /**
     * Updates an existing preparation instruction step.
     *
     * @param instructionId target instruction step ID
     * @param request instruction update request DTO
     * @param authentication active user authentication token
     * @return response entity containing updated InstructionResponse DTO
     */
    @PutMapping("/instructions/{instructionId}")
    public ResponseEntity<ApiResponse<InstructionResponse>> updateInstruction(
            @PathVariable String instructionId,
            @Valid @RequestBody InstructionRequestDTO request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        InstructionResponse response = instructionService.updateInstruction(instructionId, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success(response, "Instruction updated successfully"));
    }

    /**
     * Deletes an instruction step from a recipe.
     *
     * @param instructionId target instruction step ID
     * @param authentication active user authentication token
     * @return response entity acknowledging instruction deletion
     */
    @DeleteMapping("/instructions/{instructionId}")
    public ResponseEntity<ApiResponse<Void>> deleteInstruction(
            @PathVariable String instructionId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        instructionService.deleteInstruction(instructionId, userEmail);
        return ResponseEntity.ok(ApiResponse.success(null, "Instruction deleted successfully"));
    }
}
