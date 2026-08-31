package com.cooksync.app.ui.recipe.wizard;
import com.cooksync.app.ui.base.ViewModelFactory;
import com.cooksync.app.data.model.recipe.RecipeDraft;
import com.cooksync.app.data.model.recipe.RecipeDraftValidator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.cooksync.app.R;
import com.cooksync.app.util.DimensionUtils;
import com.cooksync.app.util.GlideUtils;
import com.cooksync.app.util.constants.DomainValues;
import com.dtos.response.tags.TagResponse;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wizard step 4: a read-only summary of the draft and a readiness checklist covering every
 * required field (title, difficulty, prep/cook time, servings, ingredients, instructions) plus
 * informational tag/cover-photo lines, each shown as a check (satisfied) or an alert (missing).
 * Since the wizard no longer gates Next on per-step validity, this is the single place the user
 * finds out what's left to fill in. The Public/Private visibility toggle lives here too; Back/
 * Draft/Publish live in {@link AddRecipeWizardActivity}'s shared bottom bar, which also observes
 * the publish outcome centrally.
 *
 * @author Yaron Serlin
 * @version 1.2
 * @since 08/08/2026
 */
public class WizardReviewFragment extends Fragment {

    private AddRecipeViewModel viewModel;

    private MaterialCardView cardCover;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvMeta;
    private LinearLayout llChecklist;
    private TextView chipPublic;
    private TextView chipPrivate;

    /**
     * Inflates this step's layout.
     *
     * @param inflater the layout inflater
     * @param container the parent view this fragment will be attached to, unused
     * @param savedInstanceState previously saved instance state, unused
     * @return the inflated view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wizard_review, container, false);
    }

    /**
     * Binds the shared {@link AddRecipeViewModel}, wires the Public/Private visibility chips,
     * and renders the initial summary and checklist.
     *
     * @param view the inflated view
     * @param savedInstanceState previously saved instance state, unused
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new ViewModelFactory()).get(AddRecipeViewModel.class);

        cardCover = view.findViewById(R.id.card_review_cover);
        ivCover = view.findViewById(R.id.iv_review_cover);
        tvTitle = view.findViewById(R.id.tv_review_title);
        tvDescription = view.findViewById(R.id.tv_review_description);
        tvMeta = view.findViewById(R.id.tv_review_meta);
        llChecklist = view.findViewById(R.id.ll_review_checklist);
        chipPublic = view.findViewById(R.id.chip_visibility_public);
        chipPrivate = view.findViewById(R.id.chip_visibility_private);

        chipPublic.setOnClickListener(v -> selectVisibility(DomainValues.VISIBILITY_PUBLIC));
        chipPrivate.setOnClickListener(v -> selectVisibility(DomainValues.VISIBILITY_PRIVATE));

        renderAll();
    }

    /**
     * Re-renders the summary and checklist, since the draft may have changed on an earlier step
     * since this fragment's view was last bound.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            renderAll();
        }
    }

    /**
     * Switches the draft's visibility and restyles the Public/Private chips to match.
     *
     * @param visibility the newly selected visibility, one of {@link DomainValues}' constants
     */
    private void selectVisibility(String visibility) {
        viewModel.setVisibility(visibility);
        styleVisibilityChips();
    }

    /** Re-renders the summary, checklist, and visibility chips from the current draft state. */
    private void renderAll() {
        renderSummary();
        renderChecklist();
        styleVisibilityChips();
    }

    /** Renders the cover photo, title, description summary, and difficulty/time/servings meta line. */
    private void renderSummary() {
        RecipeDraft draft = viewModel.getDraft();

        if (draft.primaryImageUrl != null && !draft.primaryImageUrl.isEmpty()) {
            cardCover.setVisibility(View.VISIBLE);
            GlideUtils.loadPreview(Glide.with(this), draft.primaryImageUrl, ivCover);
        } else {
            cardCover.setVisibility(View.GONE);
        }

        tvTitle.setText(draft.title == null || draft.title.trim().isEmpty()
                ? getString(R.string.wizard_review_untitled) : draft.title);

        String description = viewModel.getDescriptionSummary();
        tvDescription.setVisibility(description.isEmpty() ? View.GONE : View.VISIBLE);
        tvDescription.setText(description);

        String difficultyLabel = humanDifficulty(draft.difficulty);
        int prep = draft.prepTimeMinutes == null ? 0 : draft.prepTimeMinutes;
        int cook = draft.cookTimeMinutes == null ? 0 : draft.cookTimeMinutes;
        int servings = draft.servings == null ? 0 : draft.servings;
        tvMeta.setText(getString(R.string.wizard_review_meta_format, difficultyLabel, prep, cook, servings));
    }

    /**
     * Rebuilds the readiness checklist: one row per required or informational field, each a
     * check (satisfied) or an alert (missing/incomplete) — the sole place validation is
     * surfaced, since the wizard's Back/Next no longer block on per-step validity.
     */
    private void renderChecklist() {
        RecipeDraft draft = viewModel.getDraft();
        llChecklist.removeAllViews();

        addChecklistRow(RecipeDraftValidator.isTitleValid(draft),
                RecipeDraftValidator.isTitleValid(draft) ? draft.title : getString(R.string.wizard_checklist_title_missing));

        addChecklistRow(RecipeDraftValidator.isDifficultySet(draft),
                RecipeDraftValidator.isDifficultySet(draft)
                        ? humanDifficulty(draft.difficulty)
                        : getString(R.string.wizard_checklist_difficulty_missing));

        boolean timesOk = draft.prepTimeMinutes != null && draft.cookTimeMinutes != null;
        addChecklistRow(timesOk, timesOk
                ? getString(R.string.wizard_checklist_times_format, draft.prepTimeMinutes, draft.cookTimeMinutes)
                : getString(R.string.wizard_checklist_times_missing));

        boolean servingsOk = draft.servings != null && draft.servings >= 1;
        addChecklistRow(servingsOk, servingsOk
                ? getString(R.string.wizard_checklist_servings_format, draft.servings)
                : getString(R.string.wizard_checklist_servings_missing));

        boolean ingredientsOk = RecipeDraftValidator.areAllIngredientsValid(draft);
        addChecklistRow(ingredientsOk, ingredientsOk
                ? getString(R.string.wizard_checklist_ingredients_format, draft.ingredients.size())
                : getString(R.string.wizard_checklist_ingredients_missing));

        boolean instructionsOk = RecipeDraftValidator.areAllInstructionsValid(draft);
        addChecklistRow(instructionsOk, instructionsOk
                ? getString(R.string.wizard_checklist_steps_format, draft.instructions.size(),
                        RecipeDraftValidator.countInstructionsWithTimer(draft))
                : getString(R.string.wizard_checklist_steps_missing));

        List<String> tagNames = new ArrayList<>();
        for (TagResponse tag : draft.tags) tagNames.add(tag.name());
        tagNames.addAll(draft.pendingNewTagNames);
        if (tagNames.isEmpty()) {
            addChecklistRow(true, getString(R.string.wizard_checklist_tags_none));
        } else {
            addChecklistRow(true, getString(R.string.wizard_checklist_tags_format, tagNames.size(), String.join(", ", tagNames)));
        }

        boolean coverOk = draft.primaryImageUrl != null && !draft.primaryImageUrl.isEmpty();
        addChecklistRow(coverOk, coverOk
                ? getString(R.string.wizard_checklist_cover_added)
                : getString(R.string.wizard_checklist_cover_missing));
    }

    /**
     * Appends one checklist row (a check or alert icon plus a message) to {@link #llChecklist}.
     *
     * @param satisfied {@code true} to show a check icon and default text color, {@code false}
     *                  for an alert icon and warning color
     * @param message the row's label text
     */
    private void addChecklistRow(boolean satisfied, String message) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(4);
        rowParams.bottomMargin = dp(4);
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(17), dp(17));
        iconParams.setMarginEnd(dp(9));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(satisfied ? R.drawable.ic_check : R.drawable.ic_warning);
        icon.setColorFilter(getResources().getColor(
                satisfied ? R.color.color_accent_2_700 : R.color.color_accent_700, null));

        TextView text = new TextView(requireContext());
        text.setText(message);
        text.setTextSize(13.5f);
        text.setTextColor(getResources().getColor(
                satisfied ? R.color.color_text : R.color.color_accent_700, null));

        row.addView(icon);
        row.addView(text);
        llChecklist.addView(row);
    }

    /**
     * Converts a dp value to pixels using this fragment's context density.
     *
     * @param value the dimension in density-independent pixels
     * @return the equivalent pixel value
     */
    private int dp(int value) {
        return DimensionUtils.dpToPx(requireContext(), value);
    }

    /** Restyles the Public/Private chips to highlight whichever matches the draft's current visibility. */
    private void styleVisibilityChips() {
        String visibility = viewModel.getVisibility();
        com.cooksync.app.ui.common.ChipStyler.styleAccentChip(chipPublic, DomainValues.VISIBILITY_PUBLIC.equals(visibility));
        com.cooksync.app.ui.common.ChipStyler.styleAccentChip(chipPrivate, DomainValues.VISIBILITY_PRIVATE.equals(visibility));
    }

    /**
     * Title-cases a raw difficulty value for display (e.g. "EASY" → "Easy").
     *
     * @param difficulty the raw difficulty value, or {@code null}/empty if unset
     * @return a display-friendly capitalized version, or {@code "—"} if unset
     */
    private String humanDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isEmpty()) return "—";
        return difficulty.substring(0, 1) + difficulty.substring(1).toLowerCase(Locale.ROOT);
    }
}
