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

import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipe.RecipeVisibilityUpdateRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.RecipeImage;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.entities.User;
import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.IngredientRepository;
import com.cooksync_server.repositories.InstructionRepository;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.RecipeImageRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.RecipeSpecifications;
import com.cooksync_server.repositories.TagRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.cooksync_server.repositories.UserRepository;
import com.cooksync_server.mappers.IngredientMapper;
import com.cooksync_server.mappers.RecipeMapper;
import com.dtos.response.recipe.DescriptionBlockDTO;

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

    /**
     * Retrieves paginated slice of public recipes for feed infinite scrolling.
     *
     * @param page page index
     * @param size page size limit
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @return PagedResponse containing RecipePreviewResponse DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> getAllRecipesPaged(int page, int size, String sortBy, String difficulty, Double minRating) {
        log.debug("Fetching paginated public recipes. Page: {}, Size: {}, SortBy: {}, Difficulty: {}, MinRating: {}", page, size, sortBy, difficulty, minRating);
        Sort sort = RecipeSpecifications.resolveSortOrder(sortBy);
        Specification<Recipe> spec = RecipeSpecifications.combine(
                RecipeSpecifications.isPublicAndEnabled(),
                RecipeSpecifications.hasDifficulty(difficulty),
                RecipeSpecifications.hasMinRating(minRating)
        );
        Page<Recipe> result = recipeRepository.findAll(spec, PageRequest.of(page, size, sort));
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
    public PagedResponse<RecipePreviewResponse> searchRecipes(String keyword, String author, String ingredient, String sortBy, String difficulty, Double minRating, int page, int size) {
        log.debug("Executing recipe search. Keyword: {}, Author: {}, Ingredient: {}, SortBy: {}, Difficulty: {}, MinRating: {}, Page: {}, Size: {}", keyword, author, ingredient, sortBy, difficulty, minRating, page, size);
        Sort sort = RecipeSpecifications.resolveSortOrder(sortBy);
        Specification<Recipe> spec = RecipeSpecifications.combine(
                RecipeSpecifications.isPublicAndEnabled(),
                RecipeSpecifications.matchesUnifiedQuery(keyword),
                RecipeSpecifications.hasAuthor(author),
                RecipeSpecifications.hasIngredient(ingredient),
                RecipeSpecifications.hasDifficulty(difficulty),
                RecipeSpecifications.hasMinRating(minRating));
        Page<Recipe> result = recipeRepository.findAll(spec, PageRequest.of(page, size, sort));
        return PagedResponseMapper.toPagedResponse(result, RecipeMapper::toPreview);
    }

    /**
     * Retrieves public recipes tagged with specified tag name.
     *
     * @param tagName target tag label name
     * @param sortBy sort criterion: newest (default), rating, fastest
     * @param difficulty optional difficulty filter: EASY, MEDIUM, HARD
     * @param minRating optional minimum average rating threshold
     * @param page page index
     * @param size page size limit
     * @return list of RecipePreviewResponse DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> findRecipesByTag(String tagName, String sortBy, String difficulty, Double minRating, int page, int size) {
        log.debug("Fetching recipes by tag name: {}, SortBy: {}, Difficulty: {}, MinRating: {}, Page: {}, Size: {}", tagName, sortBy, difficulty, minRating, page, size);
        Sort sort = RecipeSpecifications.resolveSortOrder(sortBy);
        Specification<Recipe> spec = RecipeSpecifications.combine(
                RecipeSpecifications.isPublicAndEnabled(),
                RecipeSpecifications.hasTag(tagName),
                RecipeSpecifications.hasDifficulty(difficulty),
                RecipeSpecifications.hasMinRating(minRating));
        Page<Recipe> result = recipeRepository.findAll(spec, PageRequest.of(page, size, sort));
        return PagedResponseMapper.toPagedResponse(result, RecipeMapper::toPreview);
    }

    /**
     * Retrieves all recipes authored by the authenticated user.
     *
     * @param userEmail user email address
     * @param page page index
     * @param size page size limit
     * @return list of RecipePreviewResponse DTOs
     * @throws ResourceNotFoundException if no user with the given email exists
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> getMyRecipes(String userEmail, int page, int size) {
        log.debug("Fetching recipes for user email: {}, Page: {}, Size: {}", userEmail, page, size);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));
        Page<Recipe> result = recipeRepository.findByCreatedById(user.getId(), PageRequest.of(page, size));
        return PagedResponseMapper.toPagedResponse(result, RecipeMapper::toPreview);
    }

    /**
     * Retrieves the publicly visible recipes authored by a given user, for that user's public
     * profile page. Enforces the target's {@code showRecipesPublicly} preference server-side
     * (not just trusting the client to withhold the call), returning an empty page rather than
     * an error if the user opted out.
     *
     * @param userId target user ID
     * @param page page index
     * @param size page size limit
     * @return PagedResponse containing RecipePreviewResponse DTOs, empty if the user opted out
     * @throws ResourceNotFoundException if no user with the given ID exists
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> getPublicRecipesByUser(String userId, int page, int size) {
        log.debug("Fetching public recipes for user ID: {}, Page: {}, Size: {}", userId, page, size);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userId));

        if (!user.isShowRecipesPublicly()) {
            return new PagedResponse<>(List.of(), page, size, 0, 0, true);
        }

        Page<Recipe> result = recipeRepository.findByCreatedByIdAndVisibility(
                user.getId(), Recipe.Visibility.PUBLIC, PageRequest.of(page, size));
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

        applyRecipeFields(recipe, request);

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

    private Recipe.Visibility parseVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return Recipe.Visibility.PUBLIC;
        }
        return Recipe.Visibility.valueOf(visibility.toUpperCase());
    }

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

    private Map<String, Unit> fetchUnitsById(List<IngredientRequestDTO> dtoList) {
        Set<String> unitIds = dtoList.stream().map(IngredientRequestDTO::unitId).collect(Collectors.toSet());
        return unitRepository.findAllById(unitIds).stream()
                .collect(Collectors.toMap(Unit::getId, unit -> unit));
    }

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
