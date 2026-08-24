package com.cooksync.app.ui.recipe.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.lifecycle.ViewModelProvider;

import com.cooksync.app.R;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.common.FilterSheetLauncher;
import com.cooksync.app.ui.base.Navigator;
import com.cooksync.app.ui.common.OrganicToast;
import com.cooksync.app.ui.base.ViewModelFactory;
import com.cooksync.app.ui.recipe.common.RecipeListActivity;
import com.cooksync.app.ui.recipe.common.RecipeRowCardAdapter;
import com.cooksync.app.ui.recipe.detail.RecipeDetailActivity;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.tags.TagResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every recipe the current user has favorited, with search, sort/difficulty/tag
 * filtering, and a filter for favorites that carry a private note. Tapping the (always-filled)
 * heart on a card removes that recipe from favorites and offers an "Undo" toast to reverse it
 * (see {@link FavoritesViewModel#removeFavorite}). Uses the same shared list layout and row
 * card as {@link MyRecipesActivity}, differing only in data source, chips, and trailing action.
 *
 * @author Yaron Serlin
 * @version 1.2
 * @since 04/08/2026
 */
public class FavoriteRecipesActivity extends RecipeListActivity {

    private FavoritesViewModel viewModel;
    private RecipeRowCardAdapter adapter;
    private List<String> loadedTagNames = new ArrayList<>();

    private TextView chipAll;
    private TextView chipNotesOnly;

    @IdRes
    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_favorites;
    }

    /**
     * Inflates the shared list layout via {@link RecipeListActivity}, binds
     * {@link FavoritesViewModel} via {@link ViewModelFactory}, wires up the row adapter and its
     * observers, then shows the skeleton and kicks off the initial favorites and tag-catalog loads.
     *
     * @param savedInstanceState unused; this screen restores no instance state of its own
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelFactory()).get(FavoritesViewModel.class);

        initViews();
        setupObservers();

        showSkeleton(true);
        viewModel.loadFavorites();
        viewModel.loadTags();
    }

    private void initViews() {
        tvTitle.setText(R.string.favorites_title);
        ivEmptyIcon.setImageResource(R.drawable.ic_heart_filled);
        tvEmptyTitle.setText(R.string.favorites_empty_title);
        tvEmptySubtitle.setText(R.string.favorites_empty_subtitle);
        searchView.setQueryHint(getString(R.string.favorites_search_hint));
        tvSubtitle.setVisibility(View.VISIBLE);

        adapter = new RecipeRowCardAdapter();
        adapter.setTrailingAction(RecipeRowCardAdapter.TrailingAction.FAVORITE_TOGGLE);
        adapter.setShowVisibilityBadge(false);
        adapter.setListener(new RecipeRowCardAdapter.Listener() {
            @Override
            public void onRecipeClick(RecipePreviewResponse recipe) {
                Intent intent = new Intent();
                intent.putExtra(Navigator.EXTRA_RECIPE_ID, recipe.id());
                Navigator.start(FavoriteRecipesActivity.this, RecipeDetailActivity.class, intent);
            }

            @Override
            public void onTrailingActionClick(RecipePreviewResponse recipe, View anchor) {
                viewModel.removeFavorite(recipe.id());
                OrganicToast.showWithAction(FavoriteRecipesActivity.this, bottomNav, R.drawable.ic_heart_outline,
                        getString(R.string.favorites_removed), getString(R.string.action_undo), () -> viewModel.undoRemoveFavorite(recipe));
            }
        });
        rvList.setAdapter(adapter);

        setupSearchListener(viewModel::search);

        btnFilters.setOnClickListener(v ->
                FilterSheetLauncher.show(getSupportFragmentManager(), loadedTagNames, viewModel,
                        (sortBy, difficulty, tags, minRating, maxTotalTimeMinutes) -> {
                            viewModel.applyFilters(sortBy, difficulty, tags, minRating, maxTotalTimeMinutes);
                            updateFilterButton();
                        }));

        chipAll = addChip(getString(R.string.filter_all), true, () -> selectNotesFilter(false));
        chipNotesOnly = addChip(getString(R.string.favorites_with_notes_chip_format, 0L), false, () -> selectNotesFilter(true));

        setOnClearAllClickListener(() -> {
            viewModel.applyFilters("Newest", null, new ArrayList<>(), null, null);
            searchView.setQuery("", true);
            updateFilterButton();
        });
    }

    private void setupObservers() {
        viewModel.getDisplayedResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<List<RecipePreviewResponse>> success) {
                showSkeleton(false);
                List<RecipePreviewResponse> recipes = success.getData();
                adapter.setRecipes(recipes);

                tvSubtitle.setText(getString(R.string.favorites_subtitle_format,
                        viewModel.getTotalCount(), viewModel.getWithNotesCount()));
                chipNotesOnly.setText(getString(R.string.favorites_with_notes_chip_format, viewModel.getWithNotesCount()));
                updateFilterButton();

                if (!recipes.isEmpty()) {
                    hideNoResultsState();
                    emptyState.setVisibility(View.GONE);
                    rvList.setVisibility(View.VISIBLE);
                } else if (!viewModel.hasAnyFavorites()) {
                    // Genuinely no favorites yet — the static "No favorites yet" empty state.
                    emptyState.setVisibility(View.VISIBLE);
                    rvList.setVisibility(View.GONE);
                } else {
                    // Favorites exist, but the active search/filters matched none of them.
                    emptyState.setVisibility(View.GONE);
                    showNoResultsState(buildRemovableConstraints());
                }
            } else if (result instanceof ApiResult.Error<List<RecipePreviewResponse>> error) {
                showSkeleton(false);
                showError(error.getMessage(), bottomNav);
            }
        });

        viewModel.getTagsResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<List<TagResponse>> success) {
                loadedTagNames = success.getData().stream().map(TagResponse::name).collect(java.util.stream.Collectors.toList());
            }
        });
    }

    private void selectNotesFilter(boolean onlyWithNotes) {
        viewModel.setOnlyWithNotes(onlyWithNotes);
        styleChip(chipAll, !onlyWithNotes);
        styleChip(chipNotesOnly, onlyWithNotes);
    }

    @Override
    protected FilterSheetLauncher.FilterState getFilterState() {
        return viewModel;
    }

    @Override
    protected String getCurrentSearchQuery() {
        return viewModel.getCurrentQuery();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadFavorites();
    }
}
