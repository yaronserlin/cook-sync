package com.cooksync.app.ui.recipe.importing;

import android.os.Handler;
import android.os.Looper;

/**
 * Production {@link PollScheduler}, backed by a real main-thread {@link Handler}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public final class HandlerPollScheduler implements PollScheduler {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void postDelayed(Runnable task, long delayMillis) {
        handler.postDelayed(task, delayMillis);
    }

    @Override
    public void removeCallbacks(Runnable task) {
        handler.removeCallbacks(task);
    }
}
