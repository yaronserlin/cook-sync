package com.cooksync_server.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.mappers.IngredientMapper;
import com.cooksync_server.mappers.RecipeMapper;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.IngredientRepository;
import com.cooksync_server.repositories.InstructionRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeImageRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.RecipeSpecifications;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.TagRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.cooksync_server.repositories.UserRepository;
import com.cooksync_server.translation.SourceLocaleDetector;
import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeFeedRequestDTO;
import com.dtos.request.recipe.RecipeSearchRequestDTO;
import com.dtos.request.recipe.RecipeTagFilterRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.DescriptionBlockDTO;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class handling core recipe management business logic including catalog listing, search, creation, updates, and deletion.
 * Enforces transactional read-only boundaries and structured SLF4J logging for monitoring.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImp implements RecipeService{

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final InstructionRepository instructionRepository;
    private final RecipeImageRepository recipeImageRepository;
    private final TagRepository tagRepository;
    private final UnitRepository unitRepository;
    private final FavoriteRecipeRepository favoriteRecipeRepository;
    private final PersonalInstructionNoteRepository personalInstructionNoteRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final CloudinaryService cloudinaryService;
    private final TranslationCacheInvalidator translationCacheInvalidator;

    /**
     * Retrieves paginated slice of public recipes for feed infinite scrolling.
     *
     * @param request the feed's pagination, sort, and facet-filter parameters
     * @return PagedResponse containing RecipePreviewResponse DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> getAllRecipesPaged(RecipeFeedRequestDTO request) {
        log.debug("Fetching paginated public recipes. Page: {}, Size: {}, SortBy: {}, Difficulty: {}, MinRating: {}",
                request.page(), request.size(), request.sortBy(), request.difficulty(), request.minRating());
        Sort sort = RecipeSpecifications.resolveSortOrder(request.sortBy());
        Specification<Recipe> spec = RecipeSpecifications.combine(
                RecipeSpecifications.isPublicAndEnabled(),
                RecipeSpecifications.hasDifficulty(request.difficulty()),
                RecipeSpecifications.hasMinRating(request.minRating())
        );
        Page<Recipe> result = recipeRepository.findAll(spec, PageRequest.of(request.page(), request.size(), sort));
        return PagedResponseMapper.toPagedResponse(result, RecipeMapper::toPreview);
    }

    /**
     * Retrieves full detail view of a single recipe by ID using optimized fetch join.
     *
     * @param id target recipe ID
     * @return RecipeResponse DTO
     * @throws ResourceNotFoundException if no recipe with the given ID exists
     */
    @Transactional(readOnly = true)
    public RecipeResponse getRecipeById(String id) {
        log.debug("Fetching detailed recipe by ID: {}", id);
        Recipe recipe = recipeRepository.findByIdWithDetails(id)
                .orElseGet(() -> recipeRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EntityNames.RECIPE, id)));
        recipe = recipeRepository.findDescriptionBlocksByRecipeId(id).orElse(recipe);
        return RecipeMapper.toResponse(recipe);
    }

    /**
     * Unified multi-token search filtering by keyword, author, and ingredient criteria.
     *
     * @param keyword search keyword
     * @param author author name filter
     * @param ingredient ingredient filter
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @param page page index
     * @param size page size limit
     * @return list of RecipePreviewResponse DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> searchRecipes(RecipeSearchRequestDTO request) {
        log.debug("Executing recipe search. Keyword: {}, Author: {}, Ingredient: {}, SortBy: {}, Difficulty: {}, MinRating: {}, Page: {}, Size: {}",
                request.q(), request.author(), request.ingredient(), request.sortBy(), request.difficulty(), request.minRating(), request.page(), request.size());
        Sort sort = RecipeSpecifications.resolveSortOrder(request.sortBy());
        Specification<Recipe> spec = RecipeSpecifications.combine(
                RecipeSpecifications.isPublicAndEnabled(),
                RecipeSpecifications.matchesUnifiedQuery(request.q()),
                RecipeSpecifications.hasAuthor(request.author()),
                RecipeSpecifications.hasIngredient(request.ingredient()),
                RecipeSpecifications.hasDifficulty(request.difficulty()),
                RecipeSpecifications.hasMinRating(request.minRating()));
        Page<Recipe> result = recipeRepository.findAll(spec, PageRequest.of(request.page(), request.size(), sort));
        return PagedResponseMapper.toPagedResponse(result, RecipeMapper::toPreview);
    }

    /**
     * Retrieves public recipes tagged with specified tag name.
     *
     * @param tagName target tag label name
     * @param request the browse's pagination, sort, and facet-filter parameters
     * @return list of RecipePreviewResponse DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> findRecipesByTag(String tagName, RecipeTagFilterRequestDTO request) {
        log.debug("Fetching recipes by tag name: {}, SortBy: {}, Difficulty: {}, MinRating: {}, Page: {}, Size: {}",
                tagName, request.sortBy(), request.difficulty(), request.minRating(), request.page(), request.size());
        Sort sort = RecipeSpecifications.resolveSortOrder(request.sortBy());
        Specification<Recipe> spec = RecipeSpecifications.combine(
                RecipeSpecifications.isPublicAndEnabled(),
                RecipeSpecifications.hasTag(tagName),
                RecipeSpecifications.hasDifficulty(request.difficulty()),
                RecipeSpecifications.hasMinRating(request.minRating()));
        Page<Recipe> result = recipeRepository.findAll(spec, PageRequest.of(request.page(), request.size(), sort));
        return PagedResponseMapper.toPagedResponse(result, RecipeMapper::toPreview);
    }

    /**
     * Retrieves all recipes authored by the authenticated user.
     *
     * @param userEmail user email address
     * @param request pagination parameters
     * @return list of RecipePreviewResponse DTOs
     * @throws ResourceNotFoundException if no user with the given email exists
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> getMyRecipes(String userEmail, PageRequestDTO request) {
        log.debug("Fetching recipes for user email: {}, Page: {}, Size: {}", userEmail, request.page(), request.size());
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));
        Page<Recipe> result = recipeRepository.findByCreatedById(user.getId(), PageRequest.of(request.page(), request.size()));
        return PagedResponseMapper.toPagedResponse(result, RecipeMapper::toPreview);
    }

    /**
     * Retrieves the publicly visible recipes authored by a given user, for that user's public
     * profile page. Enforces the target's {@code showRecipesPublicly} preference server-side
     * (not just trusting the client to withhold the call), returning an empty page rather than
     * an error if the user opted out.
     *
     * @param userId target user ID
     * @param request pagination parameters
     * @return PagedResponse containing RecipePreviewResponse DTOs, empty if the user opted out
     * @throws ResourceNotFoundException if no user with the given ID exists
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> getPublicRecipesByUser(String userId, PageRequestDTO request) {
        log.debug("Fetching public recipes for user ID: {}, Page: {}, Size: {}", userId, request.page(), request.size());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userId));

        if (!user.isShowRecipesPublicly()) {
            return new PagedResponse<>(List.of(), request.page(), request.size(), 0, 0, true);
        }

        Page<Recipe> result = recipeRepository.findByCreatedByIdAndVisibility(
                user.getId(), Recipe.Visibility.PUBLIC, PageRequest.of(request.page(), request.size()));
        return PagedResponseMapper.toPagedResponse(result, RecipeMapper::toPreview);
    }

    /**
     * Creates a new recipe with nested ingredients, instructions, tags, and images.
     *
     * @param request recipe creation request DTO
     * @param userEmail creator user email address
     * @return created RecipeResponse DTO
     * @throws ResourceNotFoundException if the creator user, a referenced tag, or a referenced unit cannot be found
     */
    @Transactional
    public RecipeResponse createRecipe(RecipeCreateRequestDTO request, String userEmail) {
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));

        Recipe recipe = Recipe.builder()
                .createdBy(creator)
                .reviewCount(0)
                .build();
        applyRecipeFields(recipe, request);
        recipe.setSourceLocale(SourceLocaleDetector.detect(sourceLocaleSignals(request)));

        Recipe savedRecipe = recipeRepository.save(recipe);

        Map<String, Ingredient> tmpIdToIngredient = new HashMap<>();
        savedRecipe.setIngredients(saveIngredients(request.ingredients(), savedRecipe, tmpIdToIngredient));
        savedRecipe.setInstructions(saveInstructions(request.instructions(), savedRecipe, tmpIdToIngredient));
        saveImages(savedRecipe, request.primaryImageUrl());
        saveDescriptionBlocks(savedRecipe, request.descriptionBlocks());

        return RecipeMapper.toResponse(savedRecipe);
    }

    /**
     * Updates existing recipe attributes, ingredients, instructions, tags, and images.
     *
     * @param recipeId target recipe ID
     * @param request recipe update request DTO
     * @param userEmail user email address
     * @return updated RecipeResponse DTO
     * @throws ResourceNotFoundException if the recipe, acting user, a referenced tag, or a referenced unit cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the recipe owner nor an administrator
     */
    @Transactional
    public RecipeResponse updateRecipe(String recipeId, RecipeCreateRequestDTO request, String userEmail) {
        Recipe recipe = OwnershipValidator.requireOwnedResource(
                () -> recipeRepository.findById(recipeId), EntityNames.RECIPE, recipeId,
                r -> r.getCreatedBy().getId(), userRepository, userEmail,
                "You are not allowed to edit this recipe.");

        List<String> oldImageUrls = RecipeImageUtils.extractAllImageUrls(recipe);

        String oldTitle = recipe.getTitle();
        String oldDescription = recipe.getDescription();
        Set<String> oldIngredientNames = recipe.getIngredients().stream()
                .map(Ingredient::getName).collect(Collectors.toSet());
        Set<String> oldInstructionTexts = recipe.getInstructions().stream()
                .map(Instruction::getDescription).collect(Collectors.toSet());
        List<String> oldIngredientIds = recipe.getIngredients().stream()
                .map(Ingredient::getId).filter(java.util.Objects::nonNull).toList();
        List<String> oldInstructionIds = recipe.getInstructions().stream()
                .map(Instruction::getId).filter(java.util.Objects::nonNull).toList();
        List<String> oldDescriptionBlockIds = recipe.getDescriptionBlocks().stream()
                .map(DescriptionBlock::getId).filter(java.util.Objects::nonNull).toList();

        applyRecipeFields(recipe, request);

        boolean titleChanged = !java.util.Objects.equals(oldTitle, recipe.getTitle());
        boolean descriptionChanged = !java.util.Objects.equals(oldDescription, recipe.getDescription());
        boolean ingredientsChanged = !oldIngredientNames.equals(
                request.ingredients().stream().map(IngredientRequestDTO::name).collect(Collectors.toSet()));
        boolean instructionsChanged = !oldInstructionTexts.equals(
                request.instructions().stream().map(InstructionRequestDTO::description).collect(Collectors.toSet()));
        if (titleChanged || descriptionChanged || ingredientsChanged || instructionsChanged) {
            recipe.setSourceLocale(SourceLocaleDetector.detect(sourceLocaleSignals(request)));
        }

        Map<String, Ingredient> tmpIdToIngredient = new HashMap<>();
        recipe.getIngredients().clear();
        recipe.getIngredients().addAll(saveIngredients(request.ingredients(), recipe, tmpIdToIngredient));

        recipe.getInstructions().clear();
        recipe.getInstructions().addAll(saveInstructions(request.instructions(), recipe, tmpIdToIngredient));
        saveImages(recipe, request.primaryImageUrl());
        saveDescriptionBlocks(recipe, request.descriptionBlocks());

        List<String> newImageUrls = RecipeImageUtils.extractAllImageUrls(recipe);
        List<String> removedImageUrls = oldImageUrls.stream()
                .filter(url -> !newImageUrls.contains(url))
                .toList();

        cloudinaryService.deleteImages(removedImageUrls);

        // Ingredients, instructions, and description blocks are always fully replaced above
        // (cascade orphanRemoval clears and rebuilds them with new generated ids on every edit),
        // so their old translation cache rows are orphaned regardless of whether the text itself
        // changed and must be invalidated unconditionally. Title/description keep the recipe's
        // stable id, so those are only invalidated when their text actually changed.
        if (titleChanged) {
            translationCacheInvalidator.invalidate(ContentTranslation.EntityType.RECIPE_TITLE, recipeId);
        }
        if (descriptionChanged) {
            translationCacheInvalidator.invalidate(ContentTranslation.EntityType.RECIPE_DESCRIPTION, recipeId);
        }
        translationCacheInvalidator.invalidateAll(ContentTranslation.EntityType.INGREDIENT_NAME, oldIngredientIds);
        translationCacheInvalidator.invalidateAll(ContentTranslation.EntityType.INSTRUCTION_TEXT, oldInstructionIds);
        translationCacheInvalidator.invalidateAll(ContentTranslation.EntityType.RECIPE_DESCRIPTION_BLOCK, oldDescriptionBlockIds);

        return RecipeMapper.toResponse(recipeRepository.save(recipe));
    }

    /**
     * Updates only a recipe's visibility, without touching its other fields.
     *
     * @param recipeId target recipe ID
     * @param request visibility update request DTO
     * @param userEmail user email address
     * @return updated RecipeResponse DTO
     * @throws ResourceNotFoundException if the recipe or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the recipe owner nor an administrator
     */
    @Transactional
    public RecipeResponse updateVisibility(String recipeId, RecipeVisibilityUpdateRequestDTO request, String userEmail) {
        Recipe recipe = OwnershipValidator.requireOwnedResource(
                () -> recipeRepository.findById(recipeId), EntityNames.RECIPE, recipeId,
                r -> r.getCreatedBy().getId(), userRepository, userEmail,
                "You are not allowed to edit this recipe.");

        recipe.setVisibility(parseVisibility(request.visibility()));

        return RecipeMapper.toResponse(recipeRepository.save(recipe));
    }

    /**
     * Deletes a recipe by ID following ownership validation.
     *
     * @param recipeId target recipe ID
     * @param userEmail user email address
     * @throws ResourceNotFoundException if the recipe or acting user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the acting user is neither the recipe owner nor an administrator
     */
    @Transactional
    public void deleteRecipe(String recipeId, String userEmail) {
        Recipe recipe = OwnershipValidator.requireOwnedResource(
                () -> recipeRepository.findById(recipeId), EntityNames.RECIPE, recipeId,
                r -> r.getCreatedBy().getId(), userRepository, userEmail,
                "You are not allowed to delete this recipe.");

        List<String> imageUrls = RecipeImageUtils.extractAllImageUrls(recipe);
        String cleanTitle = recipe.getTitle() == null ? "" : recipe.getTitle().trim().replaceAll("[^a-zA-Z0-9_]", "_");
        String recipeFolder = cloudinaryService.buildUserFolder(recipe.getCreatedBy().getEmail(), cleanTitle);

        reviewReportRepository.deleteByRecipeId(recipeId);
        favoriteRecipeRepository.deleteByRecipeId(recipeId);
        personalInstructionNoteRepository.deleteByRecipeId(recipeId);

        cloudinaryService.deleteImages(imageUrls);
        cloudinaryService.deleteFolder(recipeFolder);

        recipeRepository.delete(recipe);
    }

    /**
     * Applies a recipe request's scalar attributes and tag set onto a recipe entity. Shared by
     * {@link #createRecipe} and {@link #updateRecipe} since both derive the same fields from the
     * same request DTO shape, differing only in whether the entity is new or already persisted.
     *
     * @param recipe target recipe entity, new or already persisted
     * @param request recipe create/update request DTO
     */
    private void applyRecipeFields(Recipe recipe, RecipeCreateRequestDTO request) {
        recipe.setTitle(request.title());
        recipe.setDescription(deriveDescription(request.descriptionBlocks()));
        recipe.setDifficulty(Recipe.Difficulty.valueOf(request.difficulty().toUpperCase()));
        recipe.setVisibility(parseVisibility(request.visibility()));
        recipe.setPrepTimeMinutes(request.prepTimeMinutes());
        recipe.setCookTimeMinutes(request.cookTimeMinutes());
        recipe.setServings(request.servings());
        recipe.setTags(fetchTags(request.tagIds()));
    }

    /**
     * Builds the priority-ordered text signals {@link SourceLocaleDetector} inspects to determine
     * a recipe's {@code sourceLocale}: the title first (most likely to be decisive and cheapest
     * to check), then the description, then every ingredient name and instruction step, so a
     * blank/ambiguous title or description still resolves correctly from the recipe's body text.
     *
     * @param request the recipe creation/update request to derive signals from
     * @return the signals, in detection priority order
     */
    private List<String> sourceLocaleSignals(RecipeCreateRequestDTO request) {
        List<String> signals = new java.util.ArrayList<>();
        signals.add(request.title());
        signals.add(deriveDescription(request.descriptionBlocks()));
        request.ingredients().forEach(ingredient -> signals.add(ingredient.name()));
        request.instructions().forEach(instruction -> signals.add(instruction.description()));
        return signals;
    }

    /**
     * @param visibility
     * @return Visibility
     */
    private Recipe.Visibility parseVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return Recipe.Visibility.PUBLIC;
        }
        return Recipe.Visibility.valueOf(visibility.toUpperCase());
    }

    /** 
     * @param recipe
     * @param primaryImageUrl
     */
    private void saveImages(Recipe recipe, String primaryImageUrl) {
        recipe.getImages().clear();
        if (primaryImageUrl != null && !primaryImageUrl.isBlank()) {
            recipe.getImages().add(RecipeImage.builder()
                    .recipe(recipe)
                    .imageUrl(primaryImageUrl)
                    .isPrimary(true)
                    .build());
        }
    }

    /**
     * Resolves a recipe's tag IDs to their persisted {@link Tag} entities.
     *
     * @param tagIds the tag IDs to resolve, or {@code null}/empty for a recipe with no tags
     * @return the resolved tag entities, in an order matching {@code tagIds}; empty if {@code tagIds} is {@code null}/empty
     * @throws ResourceNotFoundException if any ID in {@code tagIds} does not match a persisted tag
     */
    private Set<Tag> fetchTags(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new java.util.LinkedHashSet<>();
        }
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != new HashSet<>(tagIds).size()) {
            Set<String> foundIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());
            String missingId = tagIds.stream().filter(tagId -> !foundIds.contains(tagId)).findFirst().orElse(null);
            throw new ResourceNotFoundException(EntityNames.TAG, missingId);
        }
        return new java.util.LinkedHashSet<>(tags);
    }

    /**
     * Bulk-resolves the distinct measurement units referenced by a recipe's ingredient list, so
     * {@link #saveIngredients(List, Recipe, Map)} can look each one up in memory instead of
     * querying per ingredient row.
     *
     * @param dtoList the ingredient creation/update payloads whose {@code unitId} values are resolved
     * @return the found {@link Unit} entities keyed by their ID; IDs with no matching unit are simply absent
     */
    private Map<String, Unit> fetchUnitsById(List<IngredientRequestDTO> dtoList) {
        Set<String> unitIds = dtoList.stream().map(IngredientRequestDTO::unitId).collect(Collectors.toSet());
        return unitRepository.findAllById(unitIds).stream()
                .collect(Collectors.toMap(Unit::getId, unit -> unit));
    }

    /**
     * Builds the ingredient entities for a recipe from its creation/update payload, resolving
     * each one's unit and recording it under its client-supplied {@code tmpId} so instruction
     * steps can later be linked back to the ingredients they reference.
     *
     * @param dtoList the ingredient creation/update payloads to build entities from
     * @param recipe the parent recipe the built ingredients belong to
     * @param tmpIdToIngredient output map populated with an entry per ingredient that carries a
     *                          non-null {@code tmpId}, keyed by that {@code tmpId}
     * @return the built (not yet persisted) ingredient entities
     * @throws ResourceNotFoundException if any ingredient's {@code unitId} does not match a persisted unit
     */
    private Set<Ingredient> saveIngredients(List<IngredientRequestDTO> dtoList, Recipe recipe,
            Map<String, Ingredient> tmpIdToIngredient) {
        Map<String, Unit> unitsById = fetchUnitsById(dtoList);

        Set<Ingredient> ingredients = new java.util.LinkedHashSet<>();
        for (IngredientRequestDTO ingDto : dtoList) {
            Unit unit = unitsById.get(ingDto.unitId());
            if (unit == null) {
                throw new ResourceNotFoundException(EntityNames.UNIT, ingDto.unitId());
            }
            Ingredient ingredient = IngredientMapper.fromRequest(recipe, ingDto, unit);
            ingredients.add(ingredient);
            if (ingDto.tmpId() != null) {
                tmpIdToIngredient.put(ingDto.tmpId(), ingredient);
            }
        }
        return ingredients;
    }

    /**
     * Builds the instruction step entities for a recipe from its creation/update payload,
     * resolving each step's {@code ingredientIds} (client-supplied ingredient {@code tmpId}
     * values) back to the actual ingredient entities built by {@link #saveIngredients(List, Recipe, Map)}.
     *
     * @param dtoList the instruction creation/update payloads to build entities from
     * @param recipe the parent recipe the built instructions belong to
     * @param tmpIdToIngredient the ingredient entities built for this recipe, keyed by their
     *                          {@code tmpId}, as populated by {@link #saveIngredients(List, Recipe, Map)}
     * @return the built (not yet persisted) instruction entities
     */
    private Set<Instruction> saveInstructions(List<InstructionRequestDTO> dtoList, Recipe recipe,
            Map<String, Ingredient> tmpIdToIngredient) {
        Set<Instruction> instructions = new java.util.LinkedHashSet<>();
        for (InstructionRequestDTO instDto : dtoList) {
            Set<Ingredient> stepIngredients = new HashSet<>();
            if (instDto.ingredientIds() != null) {
                for (UUID ingredientId : instDto.ingredientIds()) {
                    Ingredient ingredient = tmpIdToIngredient.get(ingredientId.toString());
                    if (ingredient != null) {
                        stepIngredients.add(ingredient);
                    }
                }
            }
            Instruction instruction = Instruction.builder()
                    .recipe(recipe)
                    .stepNumber(instDto.stepNumber())
                    .description(instDto.description())
                    .imageUrl(instDto.imageUrl())
                    .hasTimer(instDto.hasTimer())
                    .timeSeconds(instDto.timeSeconds())
                    .ingredients(stepIngredients)
                    .build();
            instructions.add(instruction);
        }
        return instructions;
    }

    /**
     * Derives a flat description summary from description blocks for preview card display.
     *
     * @param blocks list of description block DTOs
     * @return first TEXT block content or empty string
     */
    private String deriveDescription(List<DescriptionBlockDTO> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        return blocks.stream()
                .filter(b -> "TEXT".equalsIgnoreCase(b.type()))
                .map(DescriptionBlockDTO::text)
                .filter(t -> t != null && !t.isBlank())
                .findFirst()
                .orElse("");
    }

    /**
     * Persists description block entities from DTO list, maintaining author-intended sort order.
     *
     * @param recipe target recipe entity
     * @param blockDTOs list of description block DTOs
     */
    private void saveDescriptionBlocks(Recipe recipe, List<DescriptionBlockDTO> blockDTOs) {
        recipe.getDescriptionBlocks().clear();
        if (blockDTOs == null || blockDTOs.isEmpty()) {
            return;
        }
        for (int i = 0; i < blockDTOs.size(); i++) {
            DescriptionBlockDTO dto = blockDTOs.get(i);
            DescriptionBlock block = DescriptionBlock.builder()
                    .recipe(recipe)
                    .type(DescriptionBlock.BlockType.valueOf(dto.type().toUpperCase()))
                    .text(dto.text())
                    .imageUrl(dto.imageUrl())
                    .caption(dto.caption())
                    .sortOrder(i)
                    .build();
            recipe.getDescriptionBlocks().add(block);
        }
    }

}
