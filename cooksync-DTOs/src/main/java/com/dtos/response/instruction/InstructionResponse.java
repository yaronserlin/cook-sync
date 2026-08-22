package com.dtos.response.instruction;

import java.util.Set;
import com.dtos.response.ingredient.IngredientResponse;

/**
 * Data Transfer Object representing a step-by-step cooking instruction in API responses.
 * Encapsulates step sequence, textual description, timer attributes, associated ingredients, and media links.
 *
 * @param id unique identifier of the instruction step record
 * @param stepNumber sequential position index of the step
 * @param description step instruction text
 * @param hasTimer boolean flag indicating whether step requires timer
 * @param timeSeconds optional timer duration in seconds
 * @param createdAt ISO formatted creation timestamp string
 * @param updatedAt ISO formatted last modification timestamp string
 * @param ingredients set of ingredient DTOs referenced by this step
 * @param imageUrl web URL for step illustration image
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record InstructionResponse(
        String id,
        int stepNumber,
        String description,
        Boolean hasTimer,
        Integer timeSeconds,
        String createdAt,
        String updatedAt,
        Set<IngredientResponse> ingredients,
        String imageUrl
) {
}
