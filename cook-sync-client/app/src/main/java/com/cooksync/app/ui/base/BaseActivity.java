package com.cooksync.app.ui.base;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cooksync.app.ui.common.OrganicToast;
import com.cooksync.app.ui.common.SkeletonHelper;

/**
 * Shared base for all Activities in the application. Centralizes the boilerplate that was
 * previously duplicated across every screen showing a loading skeleton (attach/start/stop/
 * release a {@link SkeletonHelper}) and the two {@link OrganicToast} success/error variants,
 * so feature Activities only need to call these helpers rather than re-implement them.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected SkeletonHelper skeletonHelper;
    @Nullable protected View skeletonView;

    /**
     * Locates the skeleton container by id and attaches a {@link SkeletonHelper} to it. Call
     * once, typically in {@code onCreate}, before the first {@link #showSkeleton} call.
     *
     * @param skeletonViewId the id of the root view group holding the skeleton placeholders
     */
    protected void setupSkeleton(@IdRes int skeletonViewId) {
        skeletonView = findViewById(skeletonViewId);
        if (skeletonView != null) {
            skeletonHelper = new SkeletonHelper();
            skeletonHelper.attachAll((ViewGroup) skeletonView);
        }
    }

    /**
     * Toggles between the skeleton placeholder and the real content view, starting or
     * stopping the skeleton's shimmer animation to match. No-op if {@link #setupSkeleton}
     * was never called (skeleton view not found).
     *
     * @param show {@code true} to show the skeleton and hide {@code contentView},
     *             {@code false} to hide the skeleton and show {@code contentView}
     * @param contentView the real content view to toggle opposite the skeleton, may be {@code null}
     */
    protected void showSkeleton(boolean show, @Nullable View contentView) {
        if (skeletonView == null || skeletonHelper == null) return;

        if (show) {
            skeletonView.setVisibility(View.VISIBLE);
            if (contentView != null) contentView.setVisibility(View.GONE);
            skeletonHelper.start();
        } else {
            skeletonHelper.stop();
            skeletonView.setVisibility(View.GONE);
            if (contentView != null) contentView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows a success-styled {@link OrganicToast}.
     *
     * @param message the text to display
     * @param anchor optional view to anchor the toast above, may be {@code null}
     */
    protected void showSuccess(String message, @Nullable View anchor) {
        OrganicToast.showSuccess(this, anchor, message);
    }

    /**
     * Shows an error-styled {@link OrganicToast}.
     *
     * @param message the text to display
     * @param anchor optional view to anchor the toast above, may be {@code null}
     */
    protected void showError(String message, @Nullable View anchor) {
        OrganicToast.showError(this, anchor, message);
    }

    /**
     * Displays or hides a field-level validation error message beneath a form field.
     *
     * @param tv the error {@link TextView} attached to a specific field
     * @param error the error message to display, or {@code null} to hide the view
     */
    protected void showFieldError(TextView tv, @Nullable String error) {
        if (error == null) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(error);
            tv.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Finishes this activity and applies the app's standard exit transition via
     * {@link Navigator#applyCloseTransition}. Centralizing the override here, rather than in
     * {@link Navigator} alone, means every close in the app — an explicit {@link Navigator#finish},
     * a screen's own bare {@code finish()} call, or hardware/gesture back — animates consistently,
     * since every Activity in the app extends this class.
     */
    @Override
    public void finish() {
        super.finish();
        Navigator.applyCloseTransition(this);
    }

    /**
     * Releases the {@link SkeletonHelper} attached via {@link #setupSkeleton}, if any, so its
     * shimmer animation doesn't keep running against a destroyed view hierarchy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (skeletonHelper != null) {
            skeletonHelper.release();
        }
    }
}
