package com.cooksync_server.mappers;

import com.cooksync_server.entities.Instruction;
import com.dtos.response.ingredient.IngredientResponse;
import com.dtos.response.instruction.InstructionResponse;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper utility class transforming Instruction entities into InstructionResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public final class InstructionMapper {

    private InstructionMapper() {
    }

    /**
     * Converts an Instruction entity into an InstructionResponse DTO.
     *
     * @param instruction target Instruction entity instance
     * @return populated InstructionResponse DTO instance or null
     */
    public static InstructionResponse toResponse(Instruction instruction) {
        if (instruction == null) {
            return null;
        }
        Set<IngredientResponse> ingredients = instruction.getIngredients() == null ? null
                : instruction.getIngredients().stream().map(IngredientMapper::toResponse).collect(Collectors.toSet());
        String created = MapperUtils.toIsoStringOrNull(instruction.getCreatedAt());
        String updated = MapperUtils.toIsoStringOrNull(instruction.getUpdatedAt());
        return new InstructionResponse(
                instruction.getId(),
                instruction.getStepNumber(),
                instruction.getDescription(),
                instruction.isHasTimer(),
                instruction.getTimeSeconds(),
                created,
                updated,
                ingredients,
                instruction.getImageUrl()
        );
    }
}
