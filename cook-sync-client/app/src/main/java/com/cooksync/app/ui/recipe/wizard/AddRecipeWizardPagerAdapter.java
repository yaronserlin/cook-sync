package com.cooksync.app.ui.recipe.wizard;
import com.cooksync.app.data.model.recipe.RecipeDraftValidator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Supplies {@link AddRecipeWizardActivity}'s four steps — Basics, Ingredients, Instructions,
 * Review — to its {@link androidx.viewpager2.widget.ViewPager2}, following the same
 * {@link FragmentStateAdapter} pattern {@code AdminPagerAdapter} uses for the Admin Console's
 * tabs.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
public class AddRecipeWizardPagerAdapter extends FragmentStateAdapter {

    /** Basics step index, mirroring {@link RecipeDraftValidator#STEP_BASICS}. */
    public static final int STEP_BASICS = RecipeDraftValidator.STEP_BASICS;
    /** Ingredients step index, mirroring {@link RecipeDraftValidator#STEP_INGREDIENTS}. */
    public static final int STEP_INGREDIENTS = RecipeDraftValidator.STEP_INGREDIENTS;
    /** Instructions step index, mirroring {@link RecipeDraftValidator#STEP_INSTRUCTIONS}. */
    public static final int STEP_INSTRUCTIONS = RecipeDraftValidator.STEP_INSTRUCTIONS;
    /** Review step index, mirroring {@link RecipeDraftValidator#STEP_REVIEW}. */
    public static final int STEP_REVIEW = RecipeDraftValidator.STEP_REVIEW;
    /** Total number of wizard steps. */
    public static final int STEP_COUNT = 4;

    /**
     * @param activity the hosting wizard activity
     */
    public AddRecipeWizardPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    /** @return the total number of wizard steps */
    @Override
    public int getItemCount() {
        return STEP_COUNT;
    }

    /**
     * Creates the fragment for a given wizard step position.
     *
     * @param position the step index, one of {@link #STEP_BASICS}, {@link #STEP_INGREDIENTS},
     *                 {@link #STEP_INSTRUCTIONS}, or {@link #STEP_REVIEW}
     * @return the step's fragment
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case STEP_BASICS -> new WizardBasicsFragment();
            case STEP_INGREDIENTS -> new WizardIngredientsFragment();
            case STEP_INSTRUCTIONS -> new WizardInstructionsFragment();
            default -> new WizardReviewFragment();
        };
    }
}
