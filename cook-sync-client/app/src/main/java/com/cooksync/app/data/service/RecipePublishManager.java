package com.cooksync.app.data.service;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.cooksync.app.CookSyncApplication;
import com.cooksync.app.data.datasource.local.RecipeDraftStore;
import com.cooksync.app.data.repository.MediaRepository;
import com.cooksync.app.data.repository.impl.MediaRepositoryImp;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.data.repository.impl.RecipeRepositoryImp;
import com.cooksync.app.data.repository.TagRepository;
import com.cooksync.app.data.repository.impl.TagRepositoryImp;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.domain.Event;
import com.cooksync.app.data.model.recipe.RecipeDraft;
import com.cooksync.app.data.model.recipe.RecipeDraftMapper;
import com.cooksync.app.data.model.recipe.RecipeDraftMediaHelper;
import com.cooksync.app.ui.recipe.wizard.RecipeImagePicker;
import com.cooksync.app.util.CloudinaryUploader;
import com.cooksync.app.util.SessionManager;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.tags.TagResponse;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-wide singleton managing background recipe media uploads and publishing.
 * Allows the user to tap "Publish", immediately navigate to the "My Recipes" screen,
 * and view live background progress while Cloudinary images and the backend payload are posted.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 09/08/2026
 */
public class RecipePublishManager {

    private static RecipePublishManager instance;

    /** Current state of a background publishing job. */
    public static class PublishState {
        /** Stage of a publish job, in the order a successful run passes through them. */
        public enum Status { IDLE, UPLOADING, PUBLISHING, SUCCESS, ERROR }

        public final Status status;
        public final int progress;
        public final String message;
        public final RecipeResponse recipe;
        public final String error;

        /**
         * @param status the publish job's current stage
         * @param progress completion percentage, 0-100
         * @param message a short status line for the current stage, or {@code null} if not applicable
         * @param recipe the published recipe, set only once {@code status} is {@code SUCCESS}
         * @param error a user-facing error description, set only once {@code status} is {@code ERROR}
         */
        public PublishState(Status status, int progress, String message, RecipeResponse recipe, String error) {
            this.status = status;
            this.progress = progress;
            this.message = message;
            this.recipe = recipe;
            this.error = error;
        }

        /** @return the initial, no-job-running state */
        public static PublishState idle() {
            return new PublishState(Status.IDLE, 0, null, null, null);
        }

        /**
         * @param progress completion percentage, 0-100
         * @param message a short status line describing which image is uploading
         * @return an {@code UPLOADING}-stage state
         */
        public static PublishState uploading(int progress, String message) {
            return new PublishState(Status.UPLOADING, progress, message, null, null);
        }

        /**
         * @param message a short status line describing the in-progress server call
         * @return a {@code PUBLISHING}-stage state, fixed at 90% complete
         */
        public static PublishState publishing(String message) {
            return new PublishState(Status.PUBLISHING, 90, message, null, null);
        }

        /**
         * @param recipe the successfully published recipe
         * @return a terminal {@code SUCCESS} state at 100% complete
         */
        public static PublishState success(RecipeResponse recipe) {
            return new PublishState(Status.SUCCESS, 100, "Published successfully", recipe, null);
        }

        /**
         * @param error a user-facing description of what went wrong
         * @return a terminal {@code ERROR} state
         */
        public static PublishState error(String error) {
            return new PublishState(Status.ERROR, 0, null, null, error);
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MediaRepository mediaRepository = new MediaRepositoryImp();
    private final TagRepository tagRepository = new TagRepositoryImp();
    private final RecipeRepository recipeRepository = new RecipeRepositoryImp();

    private final MutableLiveData<PublishState> publishState = new MutableLiveData<>(PublishState.idle());
    private final MutableLiveData<Event<RecipeResponse>> recipePublishedEvent = new MutableLiveData<>();

    private RecipePublishManager() {
        // Singleton
    }

    /** @return process-wide singleton instance */
    public static synchronized RecipePublishManager getInstance() {
        if (instance == null) {
            instance = new RecipePublishManager();
        }
        return instance;
    }

    /** @return LiveData exposing the current publishing state and progress */
    public LiveData<PublishState> getPublishState() {
        return publishState;
    }

    /** @return LiveData event emitted whenever a recipe is published successfully */
    public LiveData<Event<RecipeResponse>> getRecipePublishedEvent() {
        return recipePublishedEvent;
    }

    /**
     * Resets the publish state back to IDLE (e.g. after hiding the progress card).
     */
    @MainThread
    public void resetState() {
        publishState.setValue(PublishState.idle());
    }

    /**
     * Starts background publishing for the provided draft, updating progress on {@link #getPublishState()}.
     *
     * Complexity:
     * Time: O(n) where n is pending images + tags + network upload time
     * Space: O(1)
     *
     * @param draft the recipe draft to upload and publish
     */
    public void startPublish(RecipeDraft draft) {
        if (publishState.getValue() != null &&
                (publishState.getValue().status == PublishState.Status.UPLOADING ||
                 publishState.getValue().status == PublishState.Status.PUBLISHING)) {
            return;
        }

        publishState.postValue(PublishState.uploading(5, "Preparing media..."));

        executor.execute(() -> {
            try {
                // 1. Upload pending media to Cloudinary
                List<RecipeDraftMediaHelper.PendingImageUpload> pending =
                        RecipeDraftMediaHelper.collectPendingImageUploads(draft);
                int totalImages = pending.size();

                String userId = SessionManager.getInstance().getUserId();
                String userEmail = SessionManager.getInstance().getEmail();
                String recipeTitle = (draft.title == null || draft.title.isBlank()) ? "recipe" : draft.title.trim().replaceAll("[^a-zA-Z0-9_]", "_");
                String baseFolder = fetchBaseFolderSync();
                if (baseFolder == null) {
                    publishState.postValue(PublishState.error("Failed to resolve upload folder"));
                    return;
                }
                String folder = CloudinaryUploader.buildUserFolder(baseFolder, userEmail, recipeTitle);

                for (int i = 0; i < totalImages; i++) {
                    RecipeDraftMediaHelper.PendingImageUpload item = pending.get(i);
                    int itemNum = i + 1;
                    int percent = (int) (((double) i / totalImages) * 70) + 5;
                    publishState.postValue(PublishState.uploading(percent,
                            "Uploading image " + itemNum + " of " + totalImages + "..."));

                    long currentTime = System.currentTimeMillis();
                    String publicId;
                    if (item.getKind() == RecipeDraftMediaHelper.PendingImageUpload.Kind.COVER) {
                        publicId = "main_" + userId + "_" + currentTime;
                    } else if (item.getKind() == RecipeDraftMediaHelper.PendingImageUpload.Kind.DESCRIPTION_BLOCK) {
                        publicId = "description_" + userId + "_" + currentTime;
                    } else {
                        int stepNum = (item.getInstruction() != null && draft.instructions != null) ? (draft.instructions.indexOf(item.getInstruction()) + 1) : 1;
                        if (stepNum <= 0) stepNum = 1;
                        publicId = "instruction_" + stepNum + "_" + currentTime;
                    }

                    // Fetch signature synchronously
                    CloudinarySignatureResponse sig = fetchSignatureSync(folder, publicId);
                    if (sig == null) {
                        publishState.postValue(PublishState.error("Failed to acquire upload signature"));
                        return;
                    }

                    // Perform Cloudinary upload
                    String uploadedUrl = uploadImageSync(item.getLocalUri(), folder, publicId, sig);
                    if (uploadedUrl == null) {
                        publishState.postValue(PublishState.error("Failed to upload image " + itemNum));
                        return;
                    }

                    RecipeDraftMediaHelper.resolvePendingImageUpload(draft, item, uploadedUrl);
                }

                // 2. Create custom tags if any
                publishState.postValue(PublishState.publishing("Processing recipe tags..."));
                for (String tag : draft.pendingNewTagNames) {
                    TagResponse createdTag = createTagSync(tag);
                    if (createdTag != null) {
                        draft.tags.add(createdTag);
                    }
                }

                // 3. Post recipe creation or update DTO to server
                boolean isEditing = draft.editingRecipeId != null;
                publishState.postValue(PublishState.publishing(isEditing ? "Updating recipe..." : "Publishing recipe..."));
                RecipeCreateRequestDTO dto = RecipeDraftMapper.toDto(draft);
                RecipeResponse response = isEditing
                        ? updateRecipeSync(draft.editingRecipeId, dto)
                        : createRecipeSync(dto);

                if (response != null) {
                    // Only remove the auto-saved recovery draft (see AddRecipeWizardActivity#startPublishFlow)
                    // now that the recipe safely exists server-side. On any failure below or in
                    // the catch block, it stays put so the user's work is never lost.
                    RecipeDraftStore.remove(draft.draftId);
                    RecipeImagePicker.clearCache(CookSyncApplication.getAppContext());
                    publishState.postValue(PublishState.success(response));
                    recipePublishedEvent.postValue(new Event<>(response));
                } else {
                    publishState.postValue(PublishState.error(isEditing ? "Failed to update recipe on server" : "Failed to publish recipe to server"));
                }

            } catch (Exception e) {
                publishState.postValue(PublishState.error("Error publishing recipe: " + e.getMessage()));
            }
        });
    }

    /**
     * Uploads a single local image to Cloudinary and blocks the calling (background) thread
     * until the upload settles, since {@link CloudinaryUploader#upload} is itself asynchronous
     * and callback-based but this method's caller iterates images one at a time.
     *
     * @param localUri the picked/cached local ({@code file://}) URI to upload
     * @param folder target Cloudinary folder
     * @param publicId target Cloudinary public ID
     * @param signature signed upload credentials for this attempt
     * @return the uploaded asset's secure HTTPS URL, or {@code null} if the upload failed
     * @throws InterruptedException if the calling thread is interrupted while awaiting the result
     */
    private String uploadImageSync(String localUri, String folder, String publicId, CloudinarySignatureResponse signature) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        mainHandler.post(() -> CloudinaryUploader.upload(
                CookSyncApplication.getAppContext(),
                Uri.parse(localUri),
                folder,
                publicId,
                signature,
                new CloudinaryUploader.Callback() {
                    @Override
                    public void onSuccess(String secureUrl) {
                        result.set(secureUrl);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        latch.countDown();
                    }
                }
        ));
        latch.await();
        return result.get();
    }

    /**
     * Attaches a self-removing observer to a one-shot repository call, mirroring
     * {@link com.cooksync.app.ui.base.BaseViewModel#observeOnce}: skips the initial
     * {@link ApiResult.Loading} emission, invokes {@code onSettled} for the terminal
     * Success/Error value, then detaches itself so {@code target} isn't held onto once the
     * call has settled. Must be called on the main thread, since {@code observeForever}
     * requires it.
     *
     * @param <T> the payload type carried by the result
     * @param target the one-shot result stream to observe
     * @param onSettled callback invoked with the first non-Loading value
     */
    private <T> void observeOnceForever(MutableLiveData<ApiResult<T>> target, java.util.function.Consumer<ApiResult<T>> onSettled) {
        target.observeForever(new Observer<>() {
            @Override
            public void onChanged(ApiResult<T> value) {
                if (value instanceof ApiResult.Loading) {
                    return;
                }
                target.removeObserver(this);
                onSettled.accept(value);
            }
        });
    }

    /**
     * Runs a one-shot repository call synchronously from a background thread, blocking until its
     * {@link ApiResult} settles. Shared by every {@code *Sync} helper below, which otherwise
     * differed only in payload type and which repository method they invoked.
     *
     * @param <T> the payload type carried by the repository call's result
     * @param call repository invocation that posts its result onto the supplied target
     * @return the settled result's payload, or null if the call did not succeed
     * @throws InterruptedException if the calling thread is interrupted while awaiting the result
     */
    private <T> T runSync(java.util.function.Consumer<MutableLiveData<ApiResult<T>>> call) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        MutableLiveData<ApiResult<T>> target = new MutableLiveData<>();

        mainHandler.post(() -> observeOnceForever(target, res -> {
            if (res instanceof ApiResult.Success<T> s) {
                result.set(s.getData());
            }
            latch.countDown();
        }));

        call.accept(target);
        latch.await();
        return result.get();
    }

    /**
     * @return the environment-specific root Cloudinary folder, or {@code null} if the call did not succeed
     * @throws InterruptedException if the calling thread is interrupted while awaiting the result
     */
    private String fetchBaseFolderSync() throws InterruptedException {
        return runSync(mediaRepository::getBaseFolder);
    }

    /**
     * @param folder target Cloudinary folder
     * @param publicId target Cloudinary public ID
     * @return a signed upload signature, or {@code null} if the call did not succeed
     * @throws InterruptedException if the calling thread is interrupted while awaiting the result
     */
    private CloudinarySignatureResponse fetchSignatureSync(String folder, String publicId) throws InterruptedException {
        return runSync(target -> mediaRepository.getUploadSignature(folder, publicId, target));
    }

    /**
     * @param tag the tag name to create
     * @return the created tag, or {@code null} if the call did not succeed
     * @throws InterruptedException if the calling thread is interrupted while awaiting the result
     */
    private TagResponse createTagSync(String tag) throws InterruptedException {
        return runSync(target -> tagRepository.createTag(tag, target));
    }

    /**
     * @param dto the recipe to create
     * @return the created recipe, or {@code null} if the call did not succeed
     * @throws InterruptedException if the calling thread is interrupted while awaiting the result
     */
    private RecipeResponse createRecipeSync(RecipeCreateRequestDTO dto) throws InterruptedException {
        return runSync(target -> recipeRepository.createRecipe(dto, target));
    }

    /**
     * @param recipeId the recipe being edited
     * @param dto the recipe's updated content
     * @return the updated recipe, or {@code null} if the call did not succeed
     * @throws InterruptedException if the calling thread is interrupted while awaiting the result
     */
    private RecipeResponse updateRecipeSync(String recipeId, RecipeCreateRequestDTO dto) throws InterruptedException {
        return runSync(target -> recipeRepository.updateRecipe(recipeId, dto, target));
    }
}
