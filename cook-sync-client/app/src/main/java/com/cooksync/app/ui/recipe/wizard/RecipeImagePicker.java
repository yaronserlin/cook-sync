/**
 * Client-layer (Android) component of the Cloudinary image-upload feature. Wraps the recipe
 * wizard's system photo-picker and, via {@code LocalImageCache}, guards against Android's
 * short-lived {@code content://} read-permission window so a picked image survives until the
 * wizard's deferred, Publish-time upload actually runs.
 */
package com.cooksync.app.ui.recipe.wizard;

import android.content.Context;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.cooksync.app.util.LocalImageCache;

/**
 * Thin wrapper around a single system photo-picker {@link ActivityResultLauncher}, reused by
 * every image pick in the recipe creation wizard (cover photo, description photos, instruction
 * step photos). Must be constructed in a fragment's {@code onCreate} — before the fragment
 * reaches {@code STARTED} — per {@link Fragment#registerForActivityResult}'s contract.
 *
 * <p>The system picker's {@code content://} URI only grants this app read access for a short,
 * picker-defined window — not persistable via {@code takePersistableUriPermission} for modern
 * Photo Picker URIs. Since the wizard defers every Cloudinary upload until Publish (potentially
 * much later, after the user has filled in every other step), reading that original URI at
 * upload time reliably throws {@link SecurityException}. To avoid depending on that grant at
 * all, the picked file's bytes are copied into this app's private cache via
 * {@link LocalImageCache} the moment it's picked, and callers only ever see a {@code file://}
 * URI this app owns outright.</p>
 *
 * @author Yaron Serlin
 * @version 1.2
 * @since 08/08/2026
 */
public final class RecipeImagePicker {

    /** Prefix for this picker's cached files, distinguishing them in the shared app cache. */
    private static final String FILE_PREFIX = "wizard_pick_";

    /** Notified with the picked image's local URI, once it's been copied into this app's own cache. */
    public interface Listener {
        void onImagePicked(@NonNull Uri uri);
    }

    private final Fragment fragment;
    private final ActivityResultLauncher<String> launcher;
    private Listener listener;

    /**
     * Registers the underlying picker launcher against {@code fragment}.
     *
     * @param fragment the hosting fragment, mid-{@code onCreate}
     */
    public RecipeImagePicker(@NonNull Fragment fragment) {
        this.fragment = fragment;
        launcher = fragment.registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && listener != null) {
                Listener target = listener;
                LocalImageCache.copyToPrivateCache(fragment.requireContext(), uri, FILE_PREFIX, localUri -> {
                    if (localUri != null) {
                        target.onImagePicked(localUri);
                    }
                });
            }
        });
    }

    /**
     * Launches the system image picker, invoking {@code listener} once the picked image has been
     * copied into this app's private cache.
     *
     * @param listener invoked with the copied image's {@code file://} URI; not called if the
     *                  user cancels the pick, or if the copy fails
     */
    public void pick(@NonNull Listener listener) {
        this.listener = listener;
        launcher.launch("image/*");
    }

    /**
     * Deletes every cache copy this picker has made, once none of them are needed anymore — the
     * wizard's single in-flight draft either finished publishing (bytes already sent to
     * Cloudinary) or was discarded, so no local {@code file://} URI it handed out is still
     * referenced by anything.
     *
     * @param context any context; only {@link Context#getCacheDir()} is used
     */
    public static void clearCache(@NonNull Context context) {
        LocalImageCache.clearCache(context, FILE_PREFIX);
    }
}
