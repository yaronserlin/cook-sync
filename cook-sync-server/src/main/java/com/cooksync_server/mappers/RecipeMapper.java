package com.cooksync_server.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;
import com.cooksync_server.services.TranslationService;
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

        TranslationService.TranslatedText title = TranslationAccess.resolve(
                ContentTranslation.EntityType.RECIPE_TITLE, recipe.getId(), recipe.getTitle(), recipe.getSourceLocale());
        List<DescriptionBlockDTO> blocks = mapDescriptionBlocks(recipe);
        boolean isMachineTranslated = title.isMachineTranslated()
                || blocks.stream().anyMatch(DescriptionBlockDTO::isMachineTranslated);

        return new RecipeResponse(
                recipe.getId(),
                UserMapper.toResponse(recipe.getCreatedBy()),
                title.value(),
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
                blocks,
                isMachineTranslated
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
        TranslationService.TranslatedText title = TranslationAccess.resolve(
                ContentTranslation.EntityType.RECIPE_TITLE, recipe.getId(), recipe.getTitle(), recipe.getSourceLocale());
        TranslationService.TranslatedText description = TranslationAccess.resolve(
                ContentTranslation.EntityType.RECIPE_DESCRIPTION, recipe.getId(), recipe.getDescription(), recipe.getSourceLocale());

        return new RecipePreviewResponse(
                recipe.getId(),
                authorName,
                title.value(),
                description.value(),
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
                title.isMachineTranslated() || description.isMachineTranslated()
        );
    }

    /**
     * Maps recipe description blocks from entity to DTO list.
     * Falls back to synthesizing blocks from legacy flat description and non-primary images
     * when no explicit blocks are persisted on the recipe.
     *
     * @param recipe target Recipe entity
     * @return ordered list of DescriptionBlockDTO instances
     */
    private static List<DescriptionBlockDTO> mapDescriptionBlocks(Recipe recipe) {
        if (recipe.getDescriptionBlocks() != null && !recipe.getDescriptionBlocks().isEmpty()) {
            return recipe.getDescriptionBlocks().stream()
                    .map(block -> {
                        if (block.getType() != DescriptionBlock.BlockType.TEXT) {
                            return new DescriptionBlockDTO(block.getType().name(), null, block.getImageUrl(), block.getCaption(), false);
                        }
                        TranslationService.TranslatedText text = TranslationAccess.resolve(
                                ContentTranslation.EntityType.RECIPE_DESCRIPTION_BLOCK, block.getId(),
                                block.getText(), recipe.getSourceLocale());
                        return new DescriptionBlockDTO(block.getType().name(), text.value(), block.getImageUrl(),
                                block.getCaption(), text.isMachineTranslated());
                    })
                    .collect(Collectors.toList());
        }
        // Fallback: synthesize from legacy flat description + non-primary images
        List<DescriptionBlockDTO> blocks = new ArrayList<>();
        if (recipe.getDescription() != null && !recipe.getDescription().isBlank()) {
            blocks.add(new DescriptionBlockDTO("TEXT", recipe.getDescription(), null, null, false));
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
     * Maps a recipe's tag entities to their response DTOs.
     *
     * @param recipe target Recipe entity
     * @return the recipe's tags as response DTOs, or an empty list if it has none
     */
    private static List<TagResponse> mapTags(Recipe recipe) {
        return recipe.getTags() == null ? List.of()
                : recipe.getTags().stream().map(TagMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Maps a recipe's ingredient entities to their response DTOs.
     *
     * @param recipe target Recipe entity
     * @return the recipe's ingredients as response DTOs, or an empty set if it has none
     */
    private static Set<IngredientResponse> mapIngredients(Recipe recipe) {
        return recipe.getIngredients() == null ? Set.of()
                : recipe.getIngredients().stream().map(IngredientMapper::toResponse).collect(Collectors.toSet());
    }

    /**
     * Maps a recipe's instruction step entities to their response DTOs.
     *
     * @param recipe target Recipe entity
     * @return the recipe's instruction steps as response DTOs, or an empty list if it has none
     */
    private static List<InstructionResponse> mapInstructions(Recipe recipe) {
        return recipe.getInstructions() == null ? List.of()
                : recipe.getInstructions().stream().map(InstructionMapper::toResponse).collect(Collectors.toList());
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
