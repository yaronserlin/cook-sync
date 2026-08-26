package com.cooksync_server.repositories;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.Tag;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Utility class providing reusable JPA Specification criteria builders for recipe filtering and search.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    /**
     * Specification filtering public recipes created by active enabled users.
     *
     * @return JPA Specification predicate
     */
    public static Specification<Recipe> isPublicAndEnabled() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("visibility"), Recipe.Visibility.PUBLIC),
                cb.isTrue(root.get("createdBy").get("enabled"))
        );
    }

    /**
     * Specification filtering recipes authored by a matching name keyword.
     *
     * @param author author name query keyword
     * @return Specification predicate or null if search keyword is empty
     */
    public static Specification<Recipe> hasAuthor(String author) {
        if (author == null || author.isBlank()) {
            return null;
        }
        String pattern = "%" + author.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(authorName(root, cb)), pattern);
    }

    /**
     * Specification filtering recipes containing an ingredient with matching name.
     *
     * @param ingredient ingredient search string
     * @return Specification predicate or null if search string is empty
     */
    public static Specification<Recipe> hasIngredient(String ingredient) {
        if (ingredient == null || ingredient.isBlank()) {
            return null;
        }
        String pattern = "%" + ingredient.toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Recipe, Ingredient> ingredients = root.join("ingredients");
            return cb.like(cb.lower(ingredients.get("name")), pattern);
        };
    }

    /**
     * Unified multi-token search matching title, author name, tag, or ingredient name.
     *
     * @param rawQuery search query string
     * @return composite Specification predicate
     */
    public static Specification<Recipe> matchesUnifiedQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String[] tokens = rawQuery.trim().toLowerCase().split("\\s+");
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> tokenPredicates = new ArrayList<>();
            for (String token : tokens) {
                String pattern = "%" + token + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate authorMatch = cb.like(cb.lower(authorName(root, cb)), pattern);
                Predicate tagMatch = RecipeSpecifications.<Tag>existsInCollection(root, query, cb, "tags", "name", pattern);
                Predicate ingredientMatch = RecipeSpecifications.<Ingredient>existsInCollection(root, query, cb, "ingredients", "name", pattern);
                tokenPredicates.add(cb.or(titleMatch, authorMatch, tagMatch, ingredientMatch));
            }
            return cb.and(tokenPredicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Combines multiple Specifications with AND operators, skipping null entries.
     *
     * @param specs varargs array of Specification instances
     * @return combined Specification predicate
     */
    @SafeVarargs
    public static Specification<Recipe> combine(Specification<Recipe>... specs) {
        Specification<Recipe> result = (root, query, cb) -> cb.conjunction();
        for (Specification<Recipe> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }

    /**
     * Specification filtering recipes by difficulty classification level.
     *
     * @param difficulty difficulty enum string (EASY, MEDIUM, HARD)
     * @return Specification predicate or null if difficulty is empty
     */
    public static Specification<Recipe> hasDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return null;
        }
        Recipe.Difficulty difficultyEnum = Recipe.Difficulty.valueOf(difficulty.toUpperCase());
        return (root, query, cb) -> cb.equal(root.get("difficulty"), difficultyEnum);
    }

    /**
     * Specification filtering recipes with average rating greater than or equal to minimum threshold.
     *
     * @param minRating minimum average rating threshold
     * @return Specification predicate or null if minRating is null
     */
    public static Specification<Recipe> hasMinRating(Double minRating) {
        if (minRating == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"), minRating);
    }

    /**
     * Specification filtering recipes associated with a specific tag name.
     *
     * @param tagName target tag label name
     * @return Specification predicate or null if tagName is empty
     */
    public static Specification<Recipe> hasTag(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Recipe> subRoot = subquery.correlate(root);
            Join<Recipe, Tag> tagJoin = subRoot.join("tags");
            subquery.select(cb.literal(1L)).where(cb.equal(tagJoin.get("name"), tagName));
            return cb.exists(subquery);
        };
    }

    /**
     * Resolves sort order from a client-supplied sortBy string.
     * Supported values: "newest" (default), "rating", "fastest".
     *
     * @param sortBy sort criterion string
     * @return Spring Data Sort descriptor
     */
    public static Sort resolveSortOrder(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "newest".equalsIgnoreCase(sortBy)) {
            return Sort.by("createdAt").descending();
        }
        return switch (sortBy.toLowerCase()) {
            case "rating" -> Sort.by("averageRating").descending();
            case "fastest" -> Sort.by("cookTimeMinutes").ascending();
            default -> Sort.by("createdAt").descending();
        };
    }

    private static jakarta.persistence.criteria.Expression<String> authorName(Root<Recipe> root, CriteriaBuilder cb) {
        return cb.concat(cb.concat(root.get("createdBy").get("firstName"), " "), root.get("createdBy").get("lastName"));
    }

    private static <T> Predicate existsInCollection(Root<Recipe> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            String collectionAttribute, String fieldName, String pattern) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Recipe> subRoot = subquery.correlate(root);
        Join<Recipe, T> join = subRoot.join(collectionAttribute);
        subquery.select(cb.literal(1L)).where(cb.like(cb.lower(join.get(fieldName)), pattern));
        return cb.exists(subquery);
    }
}
