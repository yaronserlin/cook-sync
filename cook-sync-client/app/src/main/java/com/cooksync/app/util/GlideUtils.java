package com.cooksync.app.util;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.cooksync.app.R;

/**
 * Utility class centralizing this app's Glide image-loading configuration, so every recipe,
 * instruction, and description-block thumbnail shares the same placeholder and error drawables.
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
     * drawables — the app's standard recipe/instruction-step thumbnail treatment.
     *
     * @param requestManager the caller's {@code Glide.with(...)} result, preserving whatever
     *                       lifecycle (Activity/Fragment/Context) that call was scoped to
     * @param url the image URL to load
     * @param target the view to load the image into
     */
    public static void loadThumbnail(RequestManager requestManager, String url, ImageView target) {
        request(requestManager, url).centerCrop().into(target);
    }

    /**
     * Loads {@code url} into {@code target} with the shared placeholder/error drawables, without
     * cropping — used for preview images the layout already constrains to their natural aspect
     * ratio (e.g. the wizard's cover/description/instruction photo pickers).
     *
     * @param requestManager the caller's {@code Glide.with(...)} result, preserving whatever
     *                       lifecycle (Activity/Fragment/Context) that call was scoped to
     * @param url the image URL to load
     * @param target the view to load the image into
     */
    public static void loadPreview(RequestManager requestManager, String url, ImageView target) {
        request(requestManager, url).into(target);
    }

    /**
     * Starts a center-cropped request with the shared placeholder/error drawables, left
     * unresolved so the caller can attach its own listener before calling {@code into(...)} —
     * for a call site that needs to react to load success/failure itself.
     *
     * @param requestManager the caller's {@code Glide.with(...)} result, preserving whatever
     *                       lifecycle (Activity/Fragment/Context) that call was scoped to
     * @param url the image URL to load
     * @return a center-cropped request builder with the shared placeholder/error drawables applied
     */
    public static RequestBuilder<Drawable> requestThumbnail(RequestManager requestManager, String url) {
        return request(requestManager, url).centerCrop();
    }

    private static RequestBuilder<Drawable> request(RequestManager requestManager, String url) {
        return requestManager.load(url)
                .placeholder(R.drawable.bg_skeleton_bone)
                .error(R.drawable.ic_image_failed);
    }
}
