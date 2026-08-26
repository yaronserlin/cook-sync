package com.cooksync.app.ui.base;

import com.cooksync.app.ui.common.FilterSheetLauncher;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shared base for every ViewModel behind a screen offering the sort/difficulty/tags/rating/time
 * filter sheet (Home, Search, My Recipes, Favorites). Owns the five filter-state fields, their
 * {@link FilterSheetLauncher.FilterState} getters, {@link #applyFilters}, and the per-dimension
 * {@code removeX} methods that were previously duplicated near-identically across all four
 * ViewModels. Subclasses keep only what genuinely differs between them (their data source, any
 * extra screen-specific state such as a search query or visibility filter, and how a filter
 * change should actually take effect) by implementing {@link #onFiltersChanged()}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
public abstract class AbstractFilterableListViewModel extends BaseViewModel implements FilterSheetLauncher.FilterState {

    /**
     * Fields are {@code protected} rather than {@code private}, matching this codebase's existing
     * base-class convention (e.g. {@link BaseAdapter#items}): most subclasses only ever mutate
     * these through {@link #applyFilters}/{@code removeX} below, but {@code HomeViewModel} needs
     * direct set access for its tag-chip row's toggle-on-tap interaction, which is neither a full
     * filter-sheet apply nor a remove-only operation.
     */
    protected String currentSort = "Newest";
    protected String currentDifficulty = null;
    protected Double currentMinRating = null;
    protected Integer currentMaxTotalTimeMinutes = null;
    protected final Set<String> selectedTags = new LinkedHashSet<>();

    /** The active free-text search query, or {@code null} if none is set. */
    protected String currentQuery = null;

    /** The active search text, or {@code null} if none is set. */
    public final String getCurrentQuery() { return currentQuery; }

    /**
     * Sets the free-text search query and invokes {@link #onFiltersChanged()} to re-filter
     * already-loaded data. The default implementation suits subclasses whose full result set is
     * already loaded client-side (re-filtering is all a new query needs); a subclass whose
     * results are paginated server-side (e.g. one needing to reset pagination and issue a new
     * fetch per query) should override this instead of relying on the default.
     *
     * @param query the search text, or blank/{@code null} to clear it
     */
    public void search(String query) {
        this.currentQuery = (query == null || query.isBlank()) ? null : query.trim();
        onFiltersChanged();
    }

    @Override
    public String getCurrentSort() { return currentSort; }

    @Override
    public String getCurrentDifficulty() { return currentDifficulty; }

    @Override
    public Double getCurrentMinRating() { return currentMinRating; }

    @Override
    public Integer getCurrentMaxTotalTimeMinutes() { return currentMaxTotalTimeMinutes; }

    @Override
    public Set<String> getSelectedTags() { return Collections.unmodifiableSet(selectedTags); }

    /**
     * Applies the sort/difficulty/tags/rating/time chosen in the shared filters sheet, then
     * invokes {@link #onFiltersChanged()} so the subclass can re-filter its already-loaded data
     * or issue a new fetch, whichever its data source requires.
     *
     * @param sortBy one of "Newest", "Top Rated", "Shortest Time"
     * @param difficulty one of "Easy", "Medium", "Hard", or {@code null} for no filter
     * @param tags the selected tag names (possibly empty, never {@code null})
     * @param minRating minimum average rating threshold, or {@code null} for no filter
     * @param maxTotalTimeMinutes maximum prep+cook time in minutes, or {@code null} for no filter
     */
    public final void applyFilters(String sortBy, String difficulty, Collection<String> tags,
                                    Double minRating, Integer maxTotalTimeMinutes) {
        this.currentSort = sortBy;
        this.currentDifficulty = difficulty;
        this.currentMinRating = minRating;
        this.currentMaxTotalTimeMinutes = maxTotalTimeMinutes;
        this.selectedTags.clear();
        if (tags != null) {
            this.selectedTags.addAll(tags);
        }
        onFiltersChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void removeDifficulty() {
        currentDifficulty = null;
        onFiltersChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void removeTag(String tagName) {
        if (selectedTags.remove(tagName)) {
            onFiltersChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void removeMinRating() {
        currentMinRating = null;
        onFiltersChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void removeMaxTotalTime() {
        currentMaxTotalTimeMinutes = null;
        onFiltersChanged();
    }

    /**
     * Invoked after any filter mutation ({@link #applyFilters}, {@link #removeDifficulty},
     * {@link #removeTag}, {@link #removeMinRating}, {@link #removeMaxTotalTime}). Implementations
     * re-publish their displayed list against the new filter state — either by re-filtering
     * already-loaded data client-side, or by resetting pagination and re-fetching, depending on
     * what their data source requires.
     */
    protected abstract void onFiltersChanged();
}
