package com.cooksync_server.mappers;

import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;
import com.dtos.response.ingredient.IngredientResponse;
import com.dtos.response.instruction.InstructionResponse;
import com.dtos.response.recipe.DescriptionBlockDTO;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.review.ReviewResponse;
import com.dtos.response.tags.TagResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper utility class transforming Recipe entities into RecipeResponse and RecipePreviewResponse DTOs.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public final class RecipeMapper {

    private RecipeMapper() {
    }

    /**
     * Converts a Recipe entity into a full detail RecipeResponse DTO.
     * Maps structured description blocks; falls back to synthesizing blocks from
     * legacy flat description and non-primary images when no blocks are persisted.
     *
     * Complexity:
     * Time: O(R + T + I + S + B) where R=reviews, T=tags, I=ingredients, S=instructions, B=descriptionBlocks
     * Space: O(R + T + I + S + B)
     *
     * @param recipe target Recipe entity
     * @return populated RecipeResponse instance or null
     */
    public static RecipeResponse toResponse(Recipe recipe) {
        if (recipe == null) {
            return null;
        }
        String primaryImageUrl = resolvePrimaryImageUrl(recipe);
        List<ReviewResponse> visibleReviews = mapReviews(recipe);

        return new RecipeResponse(
                recipe.getId(),
                UserMapper.toResponse(recipe.getCreatedBy()),
                recipe.getTitle(),
                recipe.getDifficulty() == null ? null : recipe.getDifficulty().name(),
                recipe.getVisibility() == null ? null : recipe.getVisibility().name(),
                recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(),
                recipe.getServings(),
                visibleReviews.size(),
                averageRating(visibleReviews),
                visibleReviews,
                MapperUtils.toIsoStringOrNull(recipe.getCreatedAt()),
                MapperUtils.toIsoStringOrNull(recipe.getUpdatedAt()),
                mapTags(recipe),
                mapIngredients(recipe),
                mapInstructions(recipe),
                primaryImageUrl,
                mapDescriptionBlocks(recipe)
        );
    }

    /**
     * Converts a Recipe entity into a lightweight RecipePreviewResponse DTO.
     *
     * Complexity:
     * Time: O(T) where T is tag count
     * Space: O(T)
     *
     * @param recipe target Recipe entity
     * @return populated RecipePreviewResponse instance
     */
    public static RecipePreviewResponse toPreview(Recipe recipe) {
        return toPreview(recipe, false, null);
    }

    /**
     * Converts a Recipe entity into a RecipePreviewResponse with personal note text.
     *
     * Complexity:
     * Time: O(T) where T is tag count
     * Space: O(T)
     *
     * @param recipe target Recipe entity
     * @param hasPersonalNote flag indicating user attached note
     * @param personalNoteText personal note content
     * @return populated RecipePreviewResponse instance
     */
    public static RecipePreviewResponse toPreview(Recipe recipe, boolean hasPersonalNote, String personalNoteText) {
        if (recipe == null) {
            return null;
        }
        String authorName = recipe.getCreatedBy() == null ? null : recipe.getCreatedBy().getFullName();

        return new RecipePreviewResponse(
                recipe.getId(),
                authorName,
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getDifficulty() == null ? null : recipe.getDifficulty().name(),
                recipe.getVisibility() == null ? null : recipe.getVisibility().name(),
                recipe.getPrepTimeMinutes(),
                recipe.getCookTimeMinutes(),
                recipe.getReviewCount(),
                recipe.getAverageRating(),
                MapperUtils.toIsoStringOrNull(recipe.getCreatedAt()),
                mapTags(recipe),
                resolvePrimaryImageUrl(recipe),
                hasPersonalNote,
                personalNoteText
        );
    }

    /**
     * Maps recipe description blocks from entity to DTO list.
     * Falls back to synthesizing blocks from legacy flat description and non-primary images
     * when no explicit blocks are persisted on the recipe.
     *
     * Complexity:
     * Time: O(B) where B is description block count
     * Space: O(B)
     *
     * @param recipe target Recipe entity
     * @return ordered list of DescriptionBlockDTO instances
     */
    private static List<DescriptionBlockDTO> mapDescriptionBlocks(Recipe recipe) {
        if (recipe.getDescriptionBlocks() != null && !recipe.getDescriptionBlocks().isEmpty()) {
            return recipe.getDescriptionBlocks().stream()
                    .map(block -> new DescriptionBlockDTO(
                            block.getType().name(),
                            block.getText(),
                            block.getImageUrl(),
                            block.getCaption()
                    ))
                    .collect(Collectors.toList());
        }
        // Fallback: synthesize from legacy flat description + non-primary images
        List<DescriptionBlockDTO> blocks = new ArrayList<>();
        if (recipe.getDescription() != null && !recipe.getDescription().isBlank()) {
            blocks.add(new DescriptionBlockDTO("TEXT", recipe.getDescription(), null, null));
        }
        if (recipe.getImages() != null) {
            recipe.getImages().stream()
                    .filter(img -> img != null && !img.isPrimary())
                    .forEach(img -> blocks.add(new DescriptionBlockDTO("IMAGE", null, img.getImageUrl(), null)));
        }
        return blocks;
    }

    /**
     * Maps a recipe's reviews for the detail response, excluding reviews whose author has a
     * pending account-deletion request. Mirrors the same {@code hidden} filter
     * {@code ReviewRepository.findByRecipeIdAndHiddenFalseOrderByCreatedAtDesc} applies to the
     * paginated review-listing endpoint — this recipe-detail path embeds reviews directly from
     * the entity graph instead of calling that repository method, so it needs its own filter to
     * avoid leaking a deleted account's reviews here.
     *
     * @param recipe target Recipe entity
     * @return non-hidden reviews as response DTOs
     */
    private static List<ReviewResponse> mapReviews(Recipe recipe) {
        return recipe.getReviews() == null ? List.of()
                : recipe.getReviews().stream()
                        .filter(review -> review != null && !review.isHidden())
                        .map(ReviewMapper::toResponse)
                        .collect(Collectors.toList());
    }

    /**
     * Recomputes the average rating from a set of already-visible reviews, so the detail
     * response's rating stays consistent with the review list shown alongside it rather than
     * reading the recipe's denormalized {@code averageRating} column, which isn't recalculated
     * when reviews are hidden/restored by the account-deletion grace-period flow.
     *
     * @param visibleReviews the reviews being returned in this response
     * @return the average rating, or null if there are no visible reviews
     */
    private static Double averageRating(List<ReviewResponse> visibleReviews) {
        if (visibleReviews.isEmpty()) {
            return null;
        }
        return visibleReviews.stream()
                .mapToDouble(review -> review.rating().doubleValue())
                .average()
                .orElse(0.0);
    }

    private static List<TagResponse> mapTags(Recipe recipe) {
        return recipe.getTags() == null ? List.of()
                : recipe.getTags().stream().map(TagMapper::toResponse).collect(Collectors.toList());
    }

    private static Set<IngredientResponse> mapIngredients(Recipe recipe) {
        return recipe.getIngredients() == null ? Set.of()
                : recipe.getIngredients().stream().map(IngredientMapper::toResponse).collect(Collectors.toSet());
    }

    private static List<InstructionResponse> mapInstructions(Recipe recipe) {
        return recipe.getInstructions() == null ? List.of()
                : recipe.getInstructions().stream().map(InstructionMapper::toResponse).collect(Collectors.toList());
    }

    private static String resolvePrimaryImageUrl(Recipe recipe) {
        if (recipe.getImages() == null) {
            return null;
        }
        return recipe.getImages().stream()
                .filter(image -> image != null && image.isPrimary())
                .map(RecipeImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }
}
