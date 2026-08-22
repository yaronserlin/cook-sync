package com.cooksync_server.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtos.response.PagedResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.cooksync_server.entities.FavoriteRecipe;
import com.cooksync_server.entities.PersonalInstructionNote;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.mappers.RecipeMapper;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing user favorite recipe bookmark additions, removals, and retrievals.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class FavoriteService implements IFavoriteService{

    private final FavoriteRecipeRepository favoriteRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final PersonalInstructionNoteRepository personalInstructionNoteRepository;

    /**
     * Adds a recipe to the user's favorite bookmarks if not already bookmarked.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param recipeId target recipe ID
     * @param userEmail authenticated user email address
     */
    @Transactional
    public void addFavorite(String recipeId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", recipeId));

        if (!favoriteRepository.existsByUserIdAndRecipeId(user.getId(), recipe.getId())) {
            FavoriteRecipe favorite = FavoriteRecipe.builder()
                    .user(user)
                    .recipe(recipe)
                    .build();
            favoriteRepository.save(favorite);
        }
    }

    /**
     * Removes a recipe from the user's favorite bookmarks.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param recipeId target recipe ID
     * @param userEmail authenticated user email address
     */
    @Transactional
    public void removeFavorite(String recipeId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", recipeId));

        favoriteRepository.deleteByUserIdAndRecipeId(user.getId(), recipe.getId());
        personalInstructionNoteRepository.deleteByUserIdAndRecipeId(user.getId(), recipe.getId());
    }

    /**
     * Retrieves all recipe preview entries bookmarked as favorite by the user.
     *
     * Complexity:
     * Time: O(F) where F is count of bookmarked favorite recipes
     * Space: O(F)
     *
     * @param userEmail authenticated user email address
     * @param page page number
     * @param size page size
     * @return PagedResponse of RecipePreviewResponse DTOs with personal notes if present
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> getUserFavorites(String userEmail, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        Page<FavoriteRecipe> favoritesPage = favoriteRepository.findByUserId(user.getId(), PageRequest.of(page, size));

        return PagedResponseMapper.toPagedResponse(favoritesPage, fav -> {
            boolean hasNote = personalInstructionNoteRepository.existsByUserIdAndRecipeId(user.getId(), fav.getRecipe().getId());
            Optional<PersonalInstructionNote> note = personalInstructionNoteRepository
                    .findByUserIdAndRecipeIdAndInstructionIdIsNull(user.getId(), fav.getRecipe().getId());
            return RecipeMapper.toPreview(fav.getRecipe(), hasNote, note.map(PersonalInstructionNote::getNote).orElse(null));
        });
    }

    /**
     * Retrieves the publicly visible favorites of a given user, for that user's public profile
     * page. Enforces the target's {@code showFavoritesPublicly} preference server-side (not just
     * trusting the client to withhold the call), returning an empty page rather than an error if
     * the user opted out. Unlike {@link #getUserFavorites(String, int, int)}, this never attaches
     * personal note data, since another user's private notes must stay hidden regardless of this
     * setting.
     *
     * Complexity:
     * Time: O(F) where F is count of bookmarked favorite recipes
     * Space: O(F)
     *
     * @param userId target user ID
     * @param page page number
     * @param size page size
     * @return PagedResponse of RecipePreviewResponse DTOs, empty if the user opted out
     */
    @Transactional(readOnly = true)
    public PagedResponse<RecipePreviewResponse> getPublicFavoritesByUser(String userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.isShowFavoritesPublicly()) {
            return new PagedResponse<>(List.of(), page, size, 0, 0, true);
        }

        Page<FavoriteRecipe> favoritesPage = favoriteRepository.findByUserId(user.getId(), PageRequest.of(page, size));

        return PagedResponseMapper.toPagedResponse(favoritesPage, fav -> RecipeMapper.toPreview(fav.getRecipe()));
    }
}
