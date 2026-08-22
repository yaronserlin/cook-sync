package com.cooksync_server.repositories;

import com.cooksync_server.entities.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for Recipe entity operations and criteria specifications.
 * Includes EntityGraph optimizations to eliminate N+1 queries on nested collections.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, String>, JpaSpecificationExecutor<Recipe> {

    /**
     * Counts how many recipes are tagged with a specific tag ID, for the admin duplicate-tag
     * merge UI's "recipes using this tag" indicator.
     *
     * Complexity:
     * Time: O(1) (indexed join-table lookup)
     * Space: O(1)
     *
     * @param tagId target tag unique identifier
     * @return number of recipes associated with the tag
     */
    @Query("SELECT COUNT(r) FROM Recipe r JOIN r.tags t WHERE t.id = :tagId")
    long countByTagId(@Param("tagId") String tagId);

    /**
     * Retrieves all recipes authored by a specific user account ID.
     *
     * Complexity:
     * Time: O(N)
     * Space: O(N)
     *
     * @param userId unique user identifier
     * @return list of authored recipe entities
     */
    @EntityGraph(attributePaths = {"createdBy", "tags", "images"})
    Page<Recipe> findByCreatedById(String userId, Pageable pageable);

    /**
     * Retrieves the publicly visible recipes authored by a specific user account ID, for that
     * user's public profile page. Unlike {@link #findByCreatedById(String, Pageable)}, this
     * excludes the author's private recipes, since it is used to render a page other users can
     * view.
     *
     * Complexity:
     * Time: O(S) where S is page size
     * Space: O(S)
     *
     * @param userId unique user identifier
     * @param visibility visibility setting state, expected to always be {@code PUBLIC} here
     * @param pageable page request criteria
     * @return page of the user's public recipe entities
     */
    @EntityGraph(attributePaths = {"createdBy", "tags", "images"})
    Page<Recipe> findByCreatedByIdAndVisibility(String userId, Recipe.Visibility visibility, Pageable pageable);

    /**
     * Paginated retrieval of public recipes for feed infinite scrolling.
     *
     * Complexity:
     * Time: O(S) where S is page size
     * Space: O(S)
     *
     * @param visibility visibility setting state
     * @param pageable page request criteria
     * @return page of public recipe entities
     */
    @EntityGraph(attributePaths = {"createdBy", "tags", "images"})
    @Query("SELECT r FROM Recipe r WHERE r.visibility = :visibility AND r.createdBy.enabled = true")
    Page<Recipe> findByVisibility(@Param("visibility") Recipe.Visibility visibility, Pageable pageable);

    /**
     * Retrieves full recipe graph with all nested relations in a single query execution.
     * Prevents N+1 SELECT overhead when mapping full RecipeResponse DTOs.
     * Deliberately excludes {@code descriptionBlocks}: it is a {@code List} (not a {@code Set}),
     * so joining it alongside the other collection fetches here would multiply it by their
     * Cartesian product instead of being deduplicated the way the Set-typed collections are.
     * See {@link #findDescriptionBlocksByRecipeId(String)} for the companion fetch.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param id target recipe unique identifier
     * @return optional containing fully initialized Recipe entity if present
     */
    @Query("SELECT DISTINCT r FROM Recipe r " +
           "LEFT JOIN FETCH r.createdBy " +
           "LEFT JOIN FETCH r.tags " +
           "LEFT JOIN FETCH r.images " +
           "LEFT JOIN FETCH r.ingredients i " +
           "LEFT JOIN FETCH i.unit " +
           "LEFT JOIN FETCH r.instructions " +
           "WHERE r.id = :id")
    Optional<Recipe> findByIdWithDetails(@Param("id") String id);

    /**
     * Fetches a recipe's description blocks in isolation, in author-intended sort order.
     * Paired with {@link #findByIdWithDetails(String)} within the same transaction so
     * Hibernate attaches the ordered list to the already-loaded managed Recipe instance.
     *
     * Complexity:
     * Time: O(B) where B is description block count
     * Space: O(B)
     *
     * @param id target recipe unique identifier
     * @return optional containing the recipe with its description blocks initialized
     */
    @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.descriptionBlocks WHERE r.id = :id")
    Optional<Recipe> findDescriptionBlocksByRecipeId(@Param("id") String id);
}