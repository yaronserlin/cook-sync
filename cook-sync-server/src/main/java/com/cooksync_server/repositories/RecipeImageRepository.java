package com.cooksync_server.repositories;

import com.cooksync_server.entities.RecipeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA Repository interface for RecipeImage entity operations.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Repository
public interface RecipeImageRepository extends JpaRepository<RecipeImage, String> {

    /**
     * Retrieves all image entities linked to a specific recipe.
     *
     * @param recipeId target recipe unique identifier
     * @return list of recipe image entities
     */
    List<RecipeImage> findByRecipeId(String recipeId);
}