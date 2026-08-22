package com.cooksync_server.repositories;

import com.cooksync_server.entities.FavoriteRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for FavoriteRecipe entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Repository
public interface FavoriteRecipeRepository extends JpaRepository<FavoriteRecipe, String> {

    /**
     * Retrieves a paginated list of favorite recipe bookmark entries for a specific user ID.
     *
     * @param userId unique user identifier
     * @param pageable pagination and sorting information
     * @return page of favorite recipe entities
     */
    org.springframework.data.domain.Page<FavoriteRecipe> findByUserId(String userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Finds a favorite recipe relation by user ID and recipe ID.
     *
     * @param userId unique user identifier
     * @param recipeId unique recipe identifier
     * @return optional containing favorite recipe if bookmarked
     */
    Optional<FavoriteRecipe> findByUserIdAndRecipeId(String userId, String recipeId);

    /**
     * Checks if a user has bookmarked a specific recipe.
     *
     * @param userId unique user identifier
     * @param recipeId unique recipe identifier
     * @return true if favorite relation exists
     */
    boolean existsByUserIdAndRecipeId(String userId, String recipeId);

    /**
     * Deletes a favorite recipe bookmark relation by user ID and recipe ID.
     *
     * @param userId unique user identifier
     * @param recipeId unique recipe identifier
     */
    @Modifying
    @Query("DELETE FROM FavoriteRecipe f WHERE f.user.id = :userId AND f.recipe.id = :recipeId")
    void deleteByUserIdAndRecipeId(@Param("userId") String userId, @Param("recipeId") String recipeId);

    @Modifying
    @Query("DELETE FROM FavoriteRecipe f WHERE f.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") String recipeId);

    /**
     * Bulk-deletes every favorite bookmark either owned by the given user or pointing at one of
     * the given recipes. Used ahead of account-deletion purges: the first half clears the
     * deleted user's own favorites list, the second half clears other users' bookmarks on
     * recipes the deleted user authored, since {@code favorite_recipes.recipe_id} is a
     * non-nullable foreign key with no cascade delete.
     *
     * @param userId bookmarking user ID whose own favorites are included
     * @param recipeIds recipe IDs whose favorites (by any user) are included
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FavoriteRecipe f WHERE f.user.id = :userId OR f.recipe.id IN :recipeIds")
    void deleteByUserIdOrRecipeIdIn(@Param("userId") String userId, @Param("recipeIds") List<String> recipeIds);
}