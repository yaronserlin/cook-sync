package com.cooksync.app.ui.recipe.myrecipes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.lifecycle.ViewModelProvider;

import com.cooksync.app.R;
import com.cooksync.app.data.datasource.local.RecipeDraftStore;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.common.FilterSheetLauncher;
import com.cooksync.app.ui.base.Navigator;
import com.cooksync.app.ui.common.OrganicConfirmDialog;
import com.cooksync.app.ui.common.OrganicToast;
import com.cooksync.app.ui.base.ViewModelFactory;
import com.cooksync.app.ui.recipe.common.RecipeListActivity;
import com.cooksync.app.ui.recipe.common.RecipeRowCardAdapter;
import com.cooksync.app.ui.recipe.detail.RecipeDetailActivity;
import com.cooksync.app.ui.recipe.wizard.AddRecipeWizardActivity;
import com.cooksync.app.data.model.recipe.RecipeDraft;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.tags.TagResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-layer (Android) entry-point screen of the "My Recipes" feature: lists every recipe
 * (published or private) the current user has authored, fetched via {@link MyRecipesViewModel}
 * from the server's {@code GET /api/recipes/mine} endpoint and rendered as
 * {@code RecipePreviewResponse} DTOs shared with the server. Supports search, sort/difficulty/
 * tag filtering, a Public/Private chip filter, and per-recipe management actions (edit, toggle
 * visibility, delete) via an overflow menu. Owns the screen's view wiring only; all data state
 * lives in {@link MyRecipesViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.4
 * @since 04/08/2026
 */
public class MyRecipesActivity extends RecipeListActivity {

    private MyRecipesViewModel viewModel;
    private RecipeRowCardAdapter adapter;
    private List<String> loadedTagNames = new ArrayList<>();

    private TextView chipAll;
    private TextView chipPublic;
    private TextView chipPrivate;

    @IdRes
    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_my_recipes;
    }

    /**
     * Inflates the shared list layout via {@link RecipeListActivity}, binds
     * {@link MyRecipesViewModel} via {@link ViewModelFactory}, wires up the row adapter and its
     * observers, and kicks off the tag-catalog load. The recipe library itself is fetched from
     * {@link #onResume()} instead, which always runs immediately after this method too.
     *
     * @param savedInstanceState unused; this screen restores no instance state of its own
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelFactory()).get(MyRecipesViewModel.class);

        initViews();
        setupObservers();

        showSkeleton(true);
        viewModel.loadTags();
    }

    /**
     * Re-fetches the user's recipe library on every visibility change, including this screen's
     * first launch (there's no separate {@code onCreate} fetch — {@code onResume} always runs
     * right after {@code onCreate} too, so a second call there would just double the initial
     * network request), so returning here after publishing, editing, or discarding a draft
     * always shows current data.
     */
    @Override
    protected void onResume() {
        super.onResume();
        showResumableDraftIfAny();
        viewModel.loadMyRecipes();
    }

    /**
     * Shows one pinned "resumable draft" card per local, unpublished draft (see
     * {@link RecipeDraftStore}), most-recently-saved first — any number of drafts can coexist,
     * each reusing the same {@code item_recipe_draft_card} layout. Re-populated on every
     * {@link #onResume()} since drafts can be created, resumed, or discarded from
     * {@link AddRecipeWizardActivity} in between visits.
     */
    private void showResumableDraftIfAny() {
        android.view.ViewGroup draftsContainer = findViewById(R.id.drafts_container);
        draftsContainer.removeAllViews();

        List<RecipeDraft> drafts = new ArrayList<>(RecipeDraftStore.loadAll());
        drafts.sort((a, b) -> Long.compare(b.savedAtMillis, a.savedAtMillis));

        android.view.LayoutInflater inflater = getLayoutInflater();
        for (RecipeDraft draft : drafts) {
            View draftCard = inflater.inflate(R.layout.item_recipe_draft_card, draftsContainer, false);
            // Dashed shape-drawable strokes don't reliably render under hardware acceleration.
            draftCard.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            String title = draft.title == null || draft.title.trim().isEmpty()
                    ? getString(R.string.my_recipes_draft_untitled) : draft.title;
            ((TextView) draftCard.findViewById(R.id.tv_draft_title)).setText(title);

            CharSequence savedAgo = draft.savedAtMillis > 0
                    ? com.cooksync.app.util.RelativeTimeFormatter.format(draft.savedAtMillis)
                    : "";
            ((TextView) draftCard.findViewById(R.id.tv_draft_subtitle)).setText(
                    getString(R.string.wizard_draft_step_of_format, draft.lastReachedStep + 1, savedAgo));

            View.OnClickListener resume = v -> AddRecipeWizardActivity.startResumeDraft(this, draft.draftId);
            draftCard.setOnClickListener(resume);
            draftCard.findViewById(R.id.btn_resume_draft).setOnClickListener(resume);

            if (draftsContainer.getChildCount() > 0) {
                ((android.view.ViewGroup.MarginLayoutParams) draftCard.getLayoutParams()).topMargin =
                        (int) (12 * getResources().getDisplayMetrics().density);
            }
            draftsContainer.addView(draftCard);
        }
    }

    private void initViews() {
        ivEmptyIcon.setImageResource(R.drawable.ic_book);
        tvEmptyTitle.setText(R.string.my_recipes_empty_title);
        tvEmptySubtitle.setText(R.string.my_recipes_empty_subtitle);
        searchView.setQueryHint(getString(R.string.my_recipes_search_hint));
        tvSubtitle.setVisibility(View.VISIBLE);

        adapter = new RecipeRowCardAdapter();
        adapter.setTrailingAction(RecipeRowCardAdapter.TrailingAction.OPTIONS_MENU);
        adapter.setShowVisibilityBadge(true);
        adapter.setListener(new RecipeRowCardAdapter.Listener() {
            @Override
            public void onRecipeClick(RecipePreviewResponse recipe) {
                Intent intent = new Intent();
                intent.putExtra(Navigator.EXTRA_RECIPE_ID, recipe.id());
                Navigator.start(MyRecipesActivity.this, RecipeDetailActivity.class, intent);
            }

            @Override
            public void onTrailingActionClick(RecipePreviewResponse recipe, View anchor) {
                showOptionsMenu(recipe, anchor);
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

        chipAll = addChip(getString(R.string.filter_all), true, () -> selectVisibility("ALL"));
        chipPublic = addChip(getString(R.string.filter_public), false, () -> selectVisibility("PUBLIC"));
        chipPrivate = addChip(getString(R.string.filter_private), false, () -> selectVisibility("PRIVATE"));

        setOnClearAllClickListener(() -> {
            viewModel.applyFilters("Newest", null, new ArrayList<>(), null, null);
            searchView.setQuery("", true);
            updateFilterButton();
        });
    }

    private void setupObservers() {
        viewModel.getRecipesResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<List<RecipePreviewResponse>> success) {
                showSkeleton(false);
                List<RecipePreviewResponse> recipes = success.getData();
                adapter.setRecipes(recipes);

                tvTitle.setText(getString(R.string.my_recipes_title_format, viewModel.getPublishedCount()));
                tvSubtitle.setText(getString(R.string.my_recipes_subtitle_format,
                        viewModel.getTotalCount(), viewModel.getWithPrivateNotesCount()));
                updateFilterButton();

                if (!recipes.isEmpty()) {
                    hideNoResultsState();
                    emptyState.setVisibility(View.GONE);
                    rvList.setVisibility(View.VISIBLE);
                } else if (!viewModel.hasAnyRecipes()) {
                    // Genuinely no recipes yet — the static "No recipes yet" empty state.
                    emptyState.setVisibility(View.VISIBLE);
                    rvList.setVisibility(View.GONE);
                } else {
                    // Recipes exist, but the active search/filters matched none of them.
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

        // Delete waits for the server before updating the list, so both outcomes are reported
        // here. Visibility toggling below stays optimistic: it only fires for a deferred call
        // that reached the server and failed, since a success needs no signal — the list
        // already reflects it, and its own "is now public/private" toast (with Undo) is shown
        // immediately from showOptionsMenu() instead of from here.
        viewModel.getDeleteResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<Void>) {
                OrganicToast.showSuccess(this, bottomNav, getString(R.string.recipe_deleted));
            } else if (result instanceof ApiResult.Error<Void> error) {
                showError(error.getMessage(), bottomNav);
            }
        });

        viewModel.getVisibilityResult().observe(this, result -> {
            if (result instanceof ApiResult.Error<RecipeResponse> error) {
                showError(error.getMessage(), bottomNav);
            }
        });

        setupPublishProgressObserver();
    }

    /**
     * Subscribes to the process-wide {@link com.cooksync.app.data.service.RecipePublishManager}
     * singleton so a background publish begun on the wizard screen (which the user can navigate
     * away from mid-upload) is still reflected here as a progress card, even though this screen
     * didn't initiate it — including auto-refreshing the recipe list and drafts row once it
     * completes.
     */
    private void setupPublishProgressObserver() {
        com.cooksync.app.data.service.RecipePublishManager.getInstance().getPublishState().observe(this, state -> {
            if (state == null || state.status == com.cooksync.app.data.service.RecipePublishManager.PublishState.Status.IDLE) {
                View card = findViewById(R.id.card_publish_progress);
                if (card != null) card.setVisibility(View.GONE);
                return;
            }

            View card = findViewById(R.id.card_publish_progress);
            if (card == null) return;
            card.setVisibility(View.VISIBLE);

            android.widget.ProgressBar spinner = card.findViewById(R.id.pb_publish_spinner);
            android.widget.ImageView checkIcon = card.findViewById(R.id.iv_publish_success_icon);
            TextView tvTitle = card.findViewById(R.id.tv_publish_title);
            TextView tvSubtitle = card.findViewById(R.id.tv_publish_subtitle);
            TextView tvPercent = card.findViewById(R.id.tv_publish_percent);
            com.google.android.material.progressindicator.LinearProgressIndicator bar = card.findViewById(R.id.pb_publish_bar);

            switch (state.status) {
                case UPLOADING -> {
                    spinner.setVisibility(View.VISIBLE);
                    checkIcon.setVisibility(View.GONE);
                    tvTitle.setText(R.string.wizard_publish_title);
                    tvSubtitle.setText(state.message != null ? state.message : getString(R.string.wizard_publish_uploading_media));
                    tvPercent.setText(getString(R.string.wizard_publish_percent_format, state.progress));
                    bar.setProgress(state.progress);
                }
                case PUBLISHING -> {
                    spinner.setVisibility(View.VISIBLE);
                    checkIcon.setVisibility(View.GONE);
                    tvTitle.setText(R.string.wizard_publish_title);
                    tvSubtitle.setText(state.message != null ? state.message : getString(R.string.wizard_publish_processing_details));
                    tvPercent.setText(getString(R.string.wizard_publish_percent_format, state.progress));
                    bar.setProgress(state.progress);
                }
                case SUCCESS -> {
                    spinner.setVisibility(View.GONE);
                    checkIcon.setVisibility(View.VISIBLE);
                    tvTitle.setText(R.string.wizard_publish_success_title);
                    tvSubtitle.setText(R.string.wizard_publish_success_subtitle);
                    tvPercent.setText(getString(R.string.wizard_publish_percent_format, 100));
                    bar.setProgress(100);

                    viewModel.loadMyRecipes();
                    // RecipePublishManager already removed the now-published draft from
                    // RecipeDraftStore, but the drafts row rendered by onResume() before this
                    // background publish finished is stale until re-populated here.
                    showResumableDraftIfAny();
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        card.setVisibility(View.GONE);
                        com.cooksync.app.data.service.RecipePublishManager.getInstance().resetState();
                    }, 3000);
                }
                case ERROR -> {
                    spinner.setVisibility(View.GONE);
                    checkIcon.setVisibility(View.GONE);
                    tvTitle.setText(R.string.wizard_publish_failed_title);
                    String reason = state.error != null ? state.error : getString(R.string.wizard_publish_failed_default_reason);
                    tvSubtitle.setText(getString(R.string.wizard_publish_failed_subtitle_format, reason));
                    tvPercent.setText("");
                    bar.setProgress(0);

                    showResumableDraftIfAny();
                }
            }
        });
    }

    private void selectVisibility(String visibility) {
        viewModel.setVisibilityFilter(visibility);
        styleChip(chipAll, "ALL".equals(visibility));
        styleChip(chipPublic, "PUBLIC".equals(visibility));
        styleChip(chipPrivate, "PRIVATE".equals(visibility));
    }

    @Override
    protected FilterSheetLauncher.FilterState getFilterState() {
        return viewModel;
    }

    @Override
    protected String getCurrentSearchQuery() {
        return viewModel.getCurrentQuery();
    }

    private void showOptionsMenu(RecipePreviewResponse recipe, View anchor) {
        boolean isPublic = "PUBLIC".equalsIgnoreCase(recipe.visibility());

        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_my_recipe_options, popup.getMenu());
        popup.getMenu().findItem(R.id.action_toggle_visibility)
                .setTitle(isPublic ? R.string.action_make_private : R.string.action_make_public);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit_recipe) {
                androidx.lifecycle.MutableLiveData<ApiResult<RecipeResponse>> target = new androidx.lifecycle.MutableLiveData<>();
                target.observe(this, res -> {
                    if (res instanceof ApiResult.Success<RecipeResponse> s) {
                        AddRecipeWizardActivity.startEdit(MyRecipesActivity.this, s.getData());
                    } else if (res instanceof ApiResult.Error<RecipeResponse> err) {
                        showError(err.getMessage(), bottomNav);
                    }
                });
                viewModel.loadRecipeDetail(recipe.id(), target);
                return true;
            }
            if (id == R.id.action_toggle_visibility) {
                viewModel.toggleVisibility(recipe);
                String message = getString(isPublic ? R.string.recipe_now_private : R.string.recipe_now_public);
                OrganicToast.showWithAction(this, bottomNav, 0, message, getString(R.string.action_undo),
                        () -> viewModel.undoToggleVisibility(recipe));
                return true;
            }
            if (id == R.id.action_delete_recipe) {
                confirmDelete(recipe);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDelete(RecipePreviewResponse recipe) {
        OrganicConfirmDialog.show(this, getString(R.string.dialog_delete_recipe_title),
                getString(R.string.dialog_delete_recipe_message, recipe.title()),
                getString(R.string.action_delete), getString(R.string.action_cancel), true, () -> {
                    viewModel.deleteRecipe(recipe);
                });
    }
}
