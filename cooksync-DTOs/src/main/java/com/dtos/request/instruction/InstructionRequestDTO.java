package com.dtos.request.instruction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for creating or updating a recipe instruction step.
 * Encapsulates step order, textual description, optional timer configurations, and ingredient references.
 *
 * @param stepNumber the 1-based sequential position of the instruction step, must be positive
 * @param description the step-by-step instruction text, must not be blank
 * @param hasTimer flag indicating whether this step requires a countdown timer
 * @param timeSeconds optional timer duration in seconds if hasTimer is true
 * @param ingredientIds list of associated ingredient unique identifiers
 * @param imageUrl optional web URL for an illustrative instruction image
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record InstructionRequestDTO(
        @Positive(message = "Step number must be positive")
        int stepNumber,

        @NotBlank(message = "Description is required")
        String description,

        boolean hasTimer,

        Integer timeSeconds,

        List<UUID> ingredientIds,

        String imageUrl
) {
}