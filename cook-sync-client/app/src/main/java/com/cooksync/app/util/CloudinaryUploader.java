/**
 * Client-layer (Android) component of the Cloudinary image-upload feature. Wraps the Cloudinary
 * Android SDK to perform the actual direct-to-Cloudinary upload once a screen holds a signed
 * {@code CloudinarySignatureResponse} from the server's {@code CloudinaryController}; also hosts
 * {@link #buildUserFolder} so every client upload call site shares one per-user folder-path
 * format instead of reimplementing it.
 */
package com.cooksync.app.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Thin wrapper around the Cloudinary Android SDK for direct client-to-Cloudinary uploads
 * authorized by a short-lived server-issued {@link CloudinarySignatureResponse}. Centralizes
 * the one-time {@link MediaManager#init} call so every upload call site (profile avatar,
 * eventually recipe photos) doesn't need to worry about re-initialization.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 04/08/2026
 */
public final class CloudinaryUploader {

    private static volatile boolean initialized = false;

    /** Callback for the outcome of an upload. */
    public interface Callback {
        /**
         * Invoked when the upload finishes successfully.
         *
         * @param secureUrl the HTTPS URL of the uploaded asset
         */
        void onSuccess(@NonNull String secureUrl);

        /**
         * Invoked when the upload fails.
         *
         * @param message a user-facing error description
         */
        void onError(@NonNull String message);
    }

    private CloudinaryUploader() {
    }

    /**
     * Uploads the file at {@code fileUri} to Cloudinary's default folder/public-ID assignment
     * using a freshly issued signature.
     *
     * Complexity:
     * Time: O(1) plus one asynchronous network upload
     * Space: O(1)
     *
     * @param context the calling screen's context
     * @param fileUri content/file URI of the image to upload (e.g. from a photo picker)
     * @param signature signed upload credentials issued by the server
     * @param callback invoked on the main thread with the outcome
     */
    public static void upload(@NonNull Context context, @NonNull Uri fileUri,
                               @NonNull CloudinarySignatureResponse signature, @NonNull Callback callback) {
        upload(context, fileUri, null, null, signature, callback);
    }

    /**
     * Uploads the file at {@code fileUri} to Cloudinary using a freshly issued signature,
     * initializing the SDK against the signature's cloud name on first use.
     *
     * Complexity:
     * Time: O(1) plus one asynchronous network upload
     * Space: O(1)
     *
     * @param context the calling screen's context
     * @param fileUri content/file URI of the image to upload (e.g. from a photo picker)
     * @param folder target Cloudinary folder, or {@code null}/blank to use the signature's default
     * @param publicId target Cloudinary public ID, or {@code null}/blank to let Cloudinary auto-generate one
     * @param signature signed upload credentials issued by the server
     * @param callback invoked on the main thread with the outcome
     */
    public static void upload(@NonNull Context context, @NonNull Uri fileUri,
                               String folder, String publicId,
                               @NonNull CloudinarySignatureResponse signature, @NonNull Callback callback) {
        ensureInitialized(context, signature.cloudName());

        com.cloudinary.android.UploadRequest request = MediaManager.get().upload(fileUri)
                .option("api_key", signature.apiKey())
                .option("timestamp", signature.timestamp())
                .option("signature", signature.signature());

        if (folder != null && !folder.isBlank()) {
            request.option("folder", folder);
        }

        if (publicId != null && !publicId.isBlank()) {
            request.option("public_id", publicId);
        }

        request.callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        Object url = resultData.get("secure_url");
                        if (url instanceof String) {
                            callback.onSuccess((String) url);
                        } else {
                            callback.onError("Upload succeeded but no URL was returned.");
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        callback.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        callback.onError(error.getDescription());
                    }
                })
                .dispatch();
    }

    /**
     * Builds a per-user Cloudinary folder path rooted at {@code baseFolder}, in the form
     * {@code "<baseFolder>/<userEmail>[/<subPath>]"}. Mirrors the server's
     * {@code CloudinaryServiceImp#buildUserFolder}, so every client call site that needs a
     * per-user upload folder shares one implementation instead of reimplementing the format.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param baseFolder the environment-specific root folder, from {@code MediaRepository#getBaseFolder}
     * @param userEmail the owning user's email address
     * @param subPath optional trailing path segment (e.g. {@code "avatar"} or a recipe title), or
     *                {@code null}/blank to target the user's root folder
     * @return the fully qualified Cloudinary folder path
     */
    @NonNull
    public static String buildUserFolder(@NonNull String baseFolder, @NonNull String userEmail, @Nullable String subPath) {
        String folder = baseFolder + "/" + userEmail;
        return (subPath == null || subPath.isBlank()) ? folder : folder + "/" + subPath;
    }

    /**
     * Initializes {@link MediaManager} against the given cloud name exactly once per process.
     * Safe to call repeatedly with the same cloud name.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param context application context used for SDK initialization
     * @param cloudName the Cloudinary cloud name to target
     */
    private static void ensureInitialized(Context context, String cloudName) {
        if (initialized) {
            return;
        }
        synchronized (CloudinaryUploader.class) {
            if (initialized) {
                return;
            }
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", cloudName);
            MediaManager.init(context.getApplicationContext(), config);
            initialized = true;
        }
    }
}
