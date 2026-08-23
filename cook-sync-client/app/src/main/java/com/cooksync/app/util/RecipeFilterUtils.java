package com.cooksync.app.util;

import com.dtos.response.recipe.RecipePreviewResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility class for filtering and sorting recipe previews. Extracted from duplicated logic
 * that previously lived independently in both {@code HomeViewModel} and
 * {@code SearchViewModel}, so filter/sort rules are defined exactly once.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public class RecipeFilterUtils {

    /**
     * Filters {@code source} down to entries whose title or description contains {@code query}
     * (case-insensitive). Does not mutate {@code source}. Extracted from duplicated logic that
     * previously lived independently in both {@code FavoritesViewModel} and
     * {@code MyRecipesViewModel}.
     *
     * Complexity:
     * Time: O(n) where n is {@code source.size()}
     * Space: O(n) for the filtered copy
     *
     * @param source the unfiltered recipe previews
     * @param query the search text, or {@code null}/blank to skip this filter
     * @return a new, filtered list
     */
    public static List<RecipePreviewResponse> filterByQuery(List<RecipePreviewResponse> source, String query) {
        List<RecipePreviewResponse> displayed = new ArrayList<>(source);
        if (query == null || query.isBlank()) {
            return displayed;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        displayed.removeIf(r -> {
            boolean titleMatch = r.title() != null && r.title().toLowerCase(Locale.ROOT).contains(needle);
            boolean descMatch = r.description() != null && r.description().toLowerCase(Locale.ROOT).contains(needle);
            return !titleMatch && !descMatch;
        });
        return displayed;
    }

    /**
     * Applies difficulty, minimum-rating, maximum-total-time, and tag filters to
     * {@code source}, then sorts the result. Does not mutate {@code source}.
     *
     * Complexity:
     * Time: O(n log n) where n is {@code source.size()}, dominated by the final sort
     * Space: O(n) for the filtered copy
     *
     * @param source the unfiltered recipe previews
     * @param difficulty required difficulty level, or {@code null} to skip this filter
     * @param minRating minimum average rating (inclusive), or {@code null} to skip this filter
     * @param maxTotalTimeMinutes maximum prep+cook time in minutes, or {@code null} to skip this filter
     * @param selectedTags tag names every result must have, or {@code null}/empty to skip this filter
     * @param sortBy sort mode: {@code "Top Rated"}, {@code "Shortest Time"}, or anything else
     *               (including {@code null}) for newest-first
     * @return a new, filtered and sorted list
     */
    public static List<RecipePreviewResponse> applyFiltersAndSort(
            List<RecipePreviewResponse> source,
            String difficulty,
            Double minRating,
            Integer maxTotalTimeMinutes,
            Collection<String> selectedTags,
            String sortBy
    ) {
        List<RecipePreviewResponse> displayed = new ArrayList<>(source);

        if (difficulty != null) {
            displayed.removeIf(r -> r.difficulty() == null || !r.difficulty().equalsIgnoreCase(difficulty));
        }
        if (minRating != null) {
            displayed.removeIf(r -> r.averageRating() == null || r.averageRating() < minRating);
        }
        if (maxTotalTimeMinutes != null) {
            displayed.removeIf(r -> totalTimeMinutes(r) > maxTotalTimeMinutes);
        }
        if (selectedTags != null && !selectedTags.isEmpty()) {
            displayed.removeIf(r -> r.tags() == null || !selectedTags.stream().allMatch(selected ->
                    r.tags().stream().anyMatch(tag -> tag.name() != null && tag.name().equalsIgnoreCase(selected))));
        }

        String sort = Objects.requireNonNullElse(sortBy, "");
        Comparator<RecipePreviewResponse> comparator;
        if (Objects.equals(sort, "Top Rated")) {
            comparator = Comparator.comparing(
                    (RecipePreviewResponse r) -> r.averageRating() == null ? 0.0 : r.averageRating(),
                    Comparator.reverseOrder());
        } else if (Objects.equals(sort, "Shortest Time")) {
            comparator = Comparator.comparingInt(RecipeFilterUtils::totalTimeMinutes);
        } else {
            comparator = Comparator.comparing(
                    (RecipePreviewResponse r) -> r.createdAt() == null ? "" : r.createdAt(),
                    Comparator.reverseOrder());
        }
        displayed.sort(comparator);

        return displayed;
    }

    /**
     * Sums a recipe preview's prep and cook time into its total display/filter duration.
     * Extracted from duplicated logic that previously lived independently in
     * {@code RecipeCardAdapter} and {@code SearchResultAdapter}, in addition to this class's
     * own filter and sort branches above.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param recipe the recipe preview to measure
     * @return {@link RecipePreviewResponse#prepTimeMinutes()} plus {@link RecipePreviewResponse#cookTimeMinutes()}
     */
    public static int totalTimeMinutes(RecipePreviewResponse recipe) {
        return recipe.prepTimeMinutes() + recipe.cookTimeMinutes();
    }

    /**
     * Formats a recipe preview's average rating for display, matching the one-decimal style
     * used by every recipe card across the app. Extracted from duplicated logic that previously
     * lived independently in {@code RecipeCardAdapter}, {@code SearchResultAdapter}, and
     * {@code RecipeRowCardAdapter}.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param averageRating the recipe's computed average rating, or {@code null} if unrated
     * @return {@code "0.0"} if {@code averageRating} is {@code null}, otherwise its value
     *         formatted to one decimal place
     */
    public static String formatRating(Double averageRating) {
        return averageRating == null ? "0.0" : String.format(Locale.US, "%.1f", averageRating);
    }
}
