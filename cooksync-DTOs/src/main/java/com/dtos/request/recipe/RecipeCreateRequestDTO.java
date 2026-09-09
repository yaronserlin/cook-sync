package com.dtos.request.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.response.recipe.DescriptionBlockDTO;

/**
 * Data Transfer Object for creating or modifying a complete recipe entry.
 * Encapsulates metadata, structured description blocks, ingredients list, instruction steps, tags, and cover image URL.
 *
 * @param title the display title of the recipe, must not be blank
 * @param difficulty recipe skill level classification (EASY, MEDIUM, HARD), must not be blank
 * @param visibility recipe visibility setting (PUBLIC, PRIVATE), must not be blank
 * @param prepTimeMinutes preparation duration in minutes, non-negative
 * @param cookTimeMinutes active cooking duration in minutes, non-negative
 * @param servings recommended yield count, minimum 1
 * @param tagIds list of associated tag unique identifiers
 * @param ingredients list of ingredient components, minimum 1 required
 * @param instructions list of step-by-step cooking instructions, minimum 1 required
 * @param primaryImageUrl web URL of the main cover image
 * @param descriptionBlocks ordered list of structured content blocks composing the recipe description
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record RecipeCreateRequestDTO(
        @NotBlank(message = "Recipe title is required")
        @Size(max = 200, message = "Recipe title must be at most 200 characters")
        String title,

        @NotBlank(message = "Difficulty level is required (EASY, MEDIUM, HARD)")
        @Pattern(regexp = "EASY|MEDIUM|HARD", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Difficulty must be EASY, MEDIUM, or HARD")
        String difficulty,

        @NotBlank(message = "Visibility is required (PUBLIC, PRIVATE)")
        @Pattern(regexp = "PUBLIC|PRIVATE", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Visibility must be PUBLIC or PRIVATE")
        String visibility,

        @Min(value = 0, message = "Preparation time cannot be negative")
        int prepTimeMinutes,

        @Min(value = 0, message = "Cooking time cannot be negative")
        int cookTimeMinutes,

        @Min(value = 1, message = "Servings must be at least 1")
        int servings,

        @Size(max = 50, message = "A recipe can have at most 50 tags")
        List<@NotBlank(message = "Tag ID must not be blank") String> tagIds,

        @NotEmpty(message = "At least one ingredient is required")
        @Valid
        List<IngredientRequestDTO> ingredients,

        @NotEmpty(message = "At least one instruction step is required")
        @Valid
        List<InstructionRequestDTO> instructions,

        @Size(max = 2048, message = "Primary image URL must be at most 2048 characters")
        String primaryImageUrl,

        @Size(max = 50, message = "A recipe can have at most 50 description blocks")
        @Valid
        List<DescriptionBlockDTO> descriptionBlocks
) {
}
