package com.cooksync.app.util;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cooksync.app.R;

/**
 * Utility class centralizing this app's Glide image-loading configuration, so every recipe,
 * instruction, description-block, and avatar image request shares the same placeholder and error
 * drawables instead of each call site configuring Glide independently.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 22/08/2026
 */
public final class GlideUtils {

    private GlideUtils() {
    }

    /**
     * Loads {@code url} into {@code target}, center-cropped, with the shared placeholder/error
     * drawables applied — the app's standard recipe/instruction-step thumbnail treatment.
     *
     * @param requestManager the caller's {@code Glide.with(...)} result, preserving whatever
     *                       lifecycle (Activity/Fragment/Context) that call was scoped to
     * @param url the image URL to load
     * @param target the view to load the image into
     */
    public static void loadThumbnail(RequestManager requestManager, @Nullable String url, ImageView target) {
        request(requestManager, url).centerCrop().into(target);
    }

    /**
     * Loads {@code url} into {@code target} with the shared placeholder/error drawables applied,
     * without cropping — used for preview images whose layout already constrains them to their
     * natural aspect ratio (e.g. the wizard's cover/description/instruction photo pickers).
     *
     * @param requestManager the caller's {@code Glide.with(...)} result, preserving whatever
     *                       lifecycle (Activity/Fragment/Context) that call was scoped to
     * @param url the image URL to load
     * @param target the view to load the image into
     */
    public static void loadPreview(RequestManager requestManager, @Nullable String url, ImageView target) {
        request(requestManager, url).into(target);
    }

    /**
     * Starts a center-cropped request with the shared placeholder/error drawables applied, left
     * unresolved so the caller can attach its own listener before invoking {@code into(...)} —
     * for a call site that needs to react to load success or failure itself.
     *
     * @param requestManager the caller's {@code Glide.with(...)} result, preserving whatever
     *                       lifecycle (Activity/Fragment/Context) that call was scoped to
     * @param url the image URL to load
     * @return a center-cropped request builder with the shared placeholder/error drawables applied
     */
    public static RequestBuilder<Drawable> requestThumbnail(RequestManager requestManager, @Nullable String url) {
        return request(requestManager, url).centerCrop();
    }

    private static RequestBuilder<Drawable> request(RequestManager requestManager, @Nullable String url) {
        return requestManager.load(url)
                .placeholder(R.drawable.bg_skeleton_bone)
                .error(R.drawable.ic_image_failed);
    }

    /**
     * Renders either a circular avatar photo into {@code imageView} (when {@code url} is
     * present) or a fallback initials badge into {@code initialsView} — the treatment shared by
     * every screen that displays the current user's own avatar ({@code SettingsActivity},
     * {@code AccountDetailsActivity}), extracted here to replace what was previously duplicated
     * logic in each of those screens.
     *
     * @param requestManager the caller's {@code Glide.with(...)} result, preserving whatever
     *                       lifecycle (Activity/Fragment/Context) that call was scoped to
     * @param url the avatar photo URL, or {@code null}/blank to show the initials fallback
     * @param imageView the view to load the circular avatar photo into
     * @param initialsView the view to show the initials fallback in
     * @param initials the initials text to show when no photo is available
     */
    public static void renderAvatarOrInitials(RequestManager requestManager, @Nullable String url,
                                               ImageView imageView, TextView initialsView, String initials) {
        if (url == null || url.isEmpty()) {
            imageView.setImageDrawable(null);
            initialsView.setText(initials);
            initialsView.setVisibility(View.VISIBLE);
        } else {
            initialsView.setVisibility(View.GONE);
            requestManager.load(url).transform(new CircleCrop()).into(imageView);
        }
    }
}
