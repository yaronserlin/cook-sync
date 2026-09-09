package com.cooksync.app.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cooksync.app.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * The "Add recipe" FAB's entry-point choice, shared by every screen offering it (Home, My
 * Recipes, Favorites): create one from scratch in the existing wizard, or import one from a web
 * page/photo.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public final class AddRecipeEntryDialog {

    private AddRecipeEntryDialog() {
    }

    /**
     * Shows the choice dialog.
     *
     * @param context the hosting screen's context
     * @param onCreateManually invoked if the user picks "Create manually"
     * @param onImport invoked if the user picks "Import a recipe"
     */
    public static void show(@NonNull Context context, @NonNull Runnable onCreateManually, @NonNull Runnable onImport) {
        String[] options = {
                context.getString(R.string.add_recipe_option_manual),
                context.getString(R.string.add_recipe_option_import)
        };
        new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_CookSync_Dialog)
                .setTitle(R.string.add_recipe_dialog_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        onCreateManually.run();
                    } else {
                        onImport.run();
                    }
                })
                .show();
    }
}
