package com.cooksync.app.ui.recipe.importing;

/**
 * Seam around delayed-task scheduling used by {@link RecipeImportViewModel}'s status polling, so
 * tests can substitute a fake instead of a real {@link android.os.Handler} — which, being a
 * genuine Android framework class, is not usable from this project's plain-JVM unit test setup.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public interface PollScheduler {

    /**
     * Schedules {@code task} to run after {@code delayMillis}.
     *
     * @param task the task to run
     * @param delayMillis delay before running, in milliseconds
     */
    void postDelayed(Runnable task, long delayMillis);

    /**
     * Cancels a previously scheduled task, if it hasn't run yet. A no-op if it already ran or
     * was never scheduled.
     *
     * @param task the task to cancel
     */
    void removeCallbacks(Runnable task);
}
