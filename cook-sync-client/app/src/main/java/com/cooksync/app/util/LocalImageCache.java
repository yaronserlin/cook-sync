package com.cooksync.app.util;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Copies a system photo-picker's {@code content://} URI into this app's private cache, so a
 * caller that defers an upload until later holds a {@code file://} URI it owns outright instead
 * of a picker-granted URI whose read permission can expire before the deferred upload runs.
 * Shared by every screen that picks an image and only uploads it in response to a later user
 * action — the recipe wizard's cover/description/instruction photos, and the account-details
 * avatar picker.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
public final class LocalImageCache {

    /** Single background thread used to copy picked images into this app's private cache. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    /** Notified once a picked image has finished being copied into this app's private cache. */
    public interface Callback {
        /**
         * @param localUri the copied image's {@code file://} URI, or {@code null} if the copy
         *                 failed
         */
        void onCopied(@Nullable Uri localUri);
    }

    private LocalImageCache() {
    }

    /**
     * Copies {@code sourceUri} into this app's private cache directory on a background thread,
     * then invokes {@code callback} on the main thread with the resulting {@code file://} URI.
     *
     * Complexity:
     * Time: O(n) in the size of the source image, off the calling thread
     * Space: O(1)
     *
     * @param context any context; only {@link Context#getApplicationContext()} is retained
     * @param sourceUri the picker-granted URI to copy
     * @param filePrefix prefix for the cached file's name, also passed to {@link #clearCache} to
     *                    identify which cached files belong to this caller
     * @param callback invoked on the main thread with the copied file's URI, or {@code null} on
     *                 failure
     */
    public static void copyToPrivateCache(@NonNull Context context, @NonNull Uri sourceUri,
                                           @NonNull String filePrefix, @NonNull Callback callback) {
        Context appContext = context.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        EXECUTOR.execute(() -> {
            Uri localUri = copyToCacheFile(appContext, sourceUri, filePrefix);
            mainHandler.post(() -> callback.onCopied(localUri));
        });
    }

    private static Uri copyToCacheFile(Context context, Uri sourceUri, String filePrefix) {
        File outFile = new File(context.getCacheDir(), filePrefix + UUID.randomUUID() + ".jpg");
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(outFile)) {
            if (in == null) return null;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return Uri.fromFile(outFile);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Deletes every cached file previously written under the given {@code filePrefix}, once none
     * of them is needed anymore.
     *
     * Complexity:
     * Time: O(n) where n is the number of files in the app's cache directory
     * Space: O(1)
     *
     * @param context any context; only {@link Context#getCacheDir()} is used
     * @param filePrefix the prefix passed to {@link #copyToPrivateCache} for the files to remove
     */
    public static void clearCache(@NonNull Context context, @NonNull String filePrefix) {
        EXECUTOR.execute(() -> {
            File[] files = context.getCacheDir().listFiles((dir, name) -> name.startsWith(filePrefix));
            if (files == null) return;
            for (File file : files) {
                file.delete();
            }
        });
    }
}
