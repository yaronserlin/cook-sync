package com.cooksync_server.mappers;

import com.cooksync_server.entities.RecipeImportJob;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

/**
 * Mapper utility class transforming RecipeImportJob entities into RecipeImportJobResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public final class RecipeImportJobMapper {

    private RecipeImportJobMapper() {
    }

    /**
     * Converts a RecipeImportJob entity into a RecipeImportJobResponse DTO.
     *
     * @param job target RecipeImportJob entity instance
     * @return populated RecipeImportJobResponse instance
     */
    public static RecipeImportJobResponse toResponse(RecipeImportJob job) {
        return new RecipeImportJobResponse(
                job.getId(),
                job.getStatus().name(),
                job.getErrorMessage(),
                job.getResultRecipeId(),
                MapperUtils.toIsoStringOrNull(job.getCreatedAt())
        );
    }
}
