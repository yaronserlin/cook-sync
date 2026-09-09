package com.dtos.response.recipe;

import java.util.List;
import java.util.Set;

import com.dtos.response.ingredient.IngredientResponse;
import com.dtos.response.instruction.InstructionResponse;
import com.dtos.response.tags.TagResponse;
import com.dtos.response.review.ReviewResponse;
import com.dtos.response.user.PublicUserProfileResponse;

/**
 * Data Transfer Object representing complete recipe detail views.
 * Includes the author's public profile, structured description blocks, ingredient sets, step-by-step instructions, reviews, and cover image.
 *
 * @param id unique identifier of the recipe
 * @param createdBy the recipe author's public profile DTO — deliberately excludes fields
 *                   (email, admin status, account status) not appropriate to disclose to a viewer
 *                   who is not the author themself
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
 * @param isMachineTranslated whether the title or any description block shown here was produced
 *                            by on-demand machine translation rather than the recipe's own
 *                            authored text or a human-reviewed translation — surfaced so the
 *                            client can show an "auto-translated" indicator
 * @param sourceAttributionUrl the original page/photo this recipe was imported from, or
 *                             {@code null} for a manually-created recipe
 * @param sourceAttributionNote human-readable credit line paired with
 *                              {@code sourceAttributionUrl} (e.g. the source site's domain), or
 *                              {@code null} for a manually-created recipe
 * @author Yaron Serlin
 * @version 1.2
 * @since 02/08/2026
 */
public record RecipeResponse(
        String id,
        PublicUserProfileResponse createdBy,
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
        List<DescriptionBlockDTO> descriptionBlocks,
        boolean isMachineTranslated,
        String sourceAttributionUrl,
        String sourceAttributionNote
) {
}
