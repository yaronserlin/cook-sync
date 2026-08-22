package com.cooksync_server.repositories;

import com.cooksync_server.entities.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA Repository for Ingredient entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    /**
     * Retrieves all ingredient entries associated with a specific recipe.
     *
     * @param recipeId unique identifier of the target recipe
     * @return list of ingredient entities
     */
    List<Ingredient> findByRecipeId(String recipeId);
}