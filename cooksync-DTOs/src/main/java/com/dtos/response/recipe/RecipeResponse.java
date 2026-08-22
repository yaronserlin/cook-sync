package com.dtos.response.recipe;

import java.util.List;
import java.util.Set;

import com.dtos.response.ingredient.IngredientResponse;
import com.dtos.response.instruction.InstructionResponse;
import com.dtos.response.tags.TagResponse;
import com.dtos.response.review.ReviewResponse;
import com.dtos.response.user.UserResponse;

/**
 * Data Transfer Object representing complete recipe detail views.
 * Includes full author profile, structured description blocks, ingredient sets, step-by-step instructions, reviews, and cover image.
 *
 * @param id unique identifier of the recipe
 * @param createdBy user summary DTO of the recipe author
 * @param title display title of the recipe
 * @param difficulty difficulty level classification
 * @param visibility visibility configuration state
 * @param prepTimeMinutes preparation time in minutes
 * @param cookTimeMinutes active cooking time in minutes
 * @param servings recommended serving yield count
 * @param reviewCount aggregate count of submitted reviews
 * @param averageRating computed average rating score
 * @param reviews list of user review DTOs
 * @param createdAt ISO formatted creation timestamp string
 * @param updatedAt ISO formatted last update timestamp string
 * @param tags list of associated tag DTOs
 * @param ingredients set of ingredient DTOs
 * @param instructions list of step-by-step instruction DTOs
 * @param primaryImageUrl main cover image web URL
 * @param descriptionBlocks ordered list of structured content blocks composing the recipe description
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record RecipeResponse(
        String id,
        UserResponse createdBy,
        String title,
        String difficulty,
        String visibility,
        int prepTimeMinutes,
        int cookTimeMinutes,
        int servings,
        int reviewCount,
        Double averageRating,
        List<ReviewResponse> reviews,
        String createdAt,
        String updatedAt,
        List<TagResponse> tags,
        Set<IngredientResponse> ingredients,
        List<InstructionResponse> instructions,
        String primaryImageUrl,
        List<DescriptionBlockDTO> descriptionBlocks
) {
}
