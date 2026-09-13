package com.cooksync_server.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;
import com.dtos.response.ingredient.IngredientResponse;
import com.dtos.response.instruction.InstructionResponse;
import com.dtos.response.recipe.DescriptionBlockDTO;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.review.ReviewResponse;
import com.dtos.response.tags.TagResponse;

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
     * @param recipe target Recipe entity
     * @return populated RecipeResponse instance or null
     */
    public static RecipeResponse toResponse(Recipe recipe) {
        if (recipe == null) {
            return null;
        }
        String primaryImageUrl = resolvePrimaryImageUrl(recipe);
        List<ReviewResponse> visibleReviews = mapReviews(recipe);

        RecipeTranslationBundle bundle = RecipeTranslationCoordinator.resolveForDetail(recipe);
        String title = bundle.valueOf(ContentTranslation.EntityType.RECIPE_TITLE, recipe.getId(), recipe.getTitle());
        List<DescriptionBlockDTO> blocks = mapDescriptionBlocks(recipe, bundle);

        return new RecipeResponse(
                recipe.getId(),
                UserMapper.toPublicProfileResponse(recipe.getCreatedBy()),
                title,
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
                mapTags(recipe, bundle),
                mapIngredients(recipe, bundle),
                mapInstructions(recipe, bundle),
                primaryImageUrl,
                blocks,
                bundle.isMachineTranslated()
        );
    }

    /**
     * Converts a Recipe entity into a lightweight RecipePreviewResponse DTO.
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
        RecipeTranslationBundle bundle = RecipeTranslationCoordinator.resolveForPreview(recipe);
        String title = bundle.valueOf(ContentTranslation.EntityType.RECIPE_TITLE, recipe.getId(), recipe.getTitle());
        String description = bundle.valueOf(ContentTranslation.EntityType.RECIPE_DESCRIPTION, recipe.getId(), recipe.getDescription());

        return new RecipePreviewResponse(
                recipe.getId(),
                authorName,
                title,
                description,
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
                personalNoteText,
                bundle.isMachineTranslated()
        );
    }

    /**
     * Maps recipe description blocks from entity to DTO list, reading translated text from an
     * already-resolved {@link RecipeTranslationBundle}. Falls back to synthesizing blocks from
     * legacy flat description and non-primary images when no explicit blocks are persisted on the
     * recipe. Every TEXT block's {@code isMachineTranslated} reflects the bundle's overall
     * verdict rather than being tracked per block, since {@link RecipeTranslationCoordinator}
     * resolves every block as part of the same all-or-nothing attempt.
     *
     * @param recipe target Recipe entity
     * @param bundle the recipe's resolved translation bundle
     * @return ordered list of DescriptionBlockDTO instances
     */
    private static List<DescriptionBlockDTO> mapDescriptionBlocks(Recipe recipe, RecipeTranslationBundle bundle) {
        if (recipe.getDescriptionBlocks() != null && !recipe.getDescriptionBlocks().isEmpty()) {
            return recipe.getDescriptionBlocks().stream()
                    .map(block -> {
                        if (block.getType() != DescriptionBlock.BlockType.TEXT) {
                            return new DescriptionBlockDTO(block.getType().name(), null, block.getImageUrl(), block.getCaption(), false);
                        }
                        String text = bundle.valueOf(ContentTranslation.EntityType.RECIPE_DESCRIPTION_BLOCK,
                                block.getId(), block.getText());
                        return new DescriptionBlockDTO(block.getType().name(), text, block.getImageUrl(),
                                block.getCaption(), bundle.isMachineTranslated());
                    })
                    .collect(Collectors.toList());
        }
        // Fallback: synthesize from legacy flat description + non-primary images
        List<DescriptionBlockDTO> blocks = new ArrayList<>();
        if (recipe.getDescription() != null && !recipe.getDescription().isBlank()) {
            String text = bundle.valueOf(ContentTranslation.EntityType.RECIPE_DESCRIPTION, recipe.getId(), recipe.getDescription());
            blocks.add(new DescriptionBlockDTO("TEXT", text, null, null, bundle.isMachineTranslated()));
        }
        if (recipe.getImages() != null) {
            recipe.getImages().stream()
                    .filter(img -> img != null && !img.isPrimary())
                    .forEach(img -> blocks.add(new DescriptionBlockDTO("IMAGE", null, img.getImageUrl(), null, false)));
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
        return ReviewMapper.averageRating(visibleReviews.stream().map(ReviewResponse::rating).toList());
    }

    /**
     * Maps a recipe's tag entities to their response DTOs, independently of any translation
     * bundle. Used by {@link #toPreview} — preview-card tags stay out of the preview's smaller
     * (title+description only) translation attempt, since they're used only for client-side
     * filter-name matching and never rendered on the card itself.
     *
     * @param recipe target Recipe entity
     * @return the recipe's tags as response DTOs, or an empty list if it has none
     */
    private static List<TagResponse> mapTags(Recipe recipe) {
        return recipe.getTags() == null ? List.of()
                : recipe.getTags().stream().map(TagMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Maps a recipe's tag entities to their response DTOs, reading each tag's translated name
     * from an already-resolved {@link RecipeTranslationBundle}.
     *
     * @param recipe target Recipe entity
     * @param bundle the recipe's resolved translation bundle
     * @return the recipe's tags as response DTOs, or an empty list if it has none
     */
    private static List<TagResponse> mapTags(Recipe recipe, RecipeTranslationBundle bundle) {
        return recipe.getTags() == null ? List.of()
                : recipe.getTags().stream().map(tag -> TagMapper.toResponse(tag, bundle)).collect(Collectors.toList());
    }

    /**
     * Maps a recipe's ingredient entities to their response DTOs, reading each ingredient's
     * translated name from an already-resolved {@link RecipeTranslationBundle}.
     *
     * @param recipe target Recipe entity
     * @param bundle the recipe's resolved translation bundle
     * @return the recipe's ingredients as response DTOs, or an empty set if it has none
     */
    private static Set<IngredientResponse> mapIngredients(Recipe recipe, RecipeTranslationBundle bundle) {
        return recipe.getIngredients() == null ? Set.of()
                : recipe.getIngredients().stream()
                        .map(ingredient -> IngredientMapper.toResponse(ingredient, bundle))
                        .collect(Collectors.toSet());
    }

    /**
     * Maps a recipe's instruction step entities to their response DTOs, reading each
     * instruction's translated text from an already-resolved {@link RecipeTranslationBundle}.
     *
     * @param recipe target Recipe entity
     * @param bundle the recipe's resolved translation bundle
     * @return the recipe's instruction steps as response DTOs, or an empty list if it has none
     */
    private static List<InstructionResponse> mapInstructions(Recipe recipe, RecipeTranslationBundle bundle) {
        return recipe.getInstructions() == null ? List.of()
                : recipe.getInstructions().stream()
                        .map(instruction -> InstructionMapper.toResponse(instruction, bundle))
                        .collect(Collectors.toList());
    }

    /**
     * Finds the URL of a recipe's cover image, i.e. the one image entity flagged as primary.
     *
     * @param recipe target Recipe entity
     * @return the primary image's URL, or {@code null} if the recipe has no images or none is flagged primary
     */
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
