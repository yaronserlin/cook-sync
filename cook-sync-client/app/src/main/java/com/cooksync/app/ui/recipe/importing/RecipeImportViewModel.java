package com.cooksync.app.ui.recipe.importing;

import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.CookSyncApplication;
import com.cooksync.app.R;
import com.cooksync.app.data.repository.MediaRepository;
import com.cooksync.app.data.repository.RecipeImportRepository;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.domain.Event;
import com.cooksync.app.ui.base.BaseViewModel;
import com.cooksync.app.util.CloudinaryUploader;
import com.cooksync.app.util.SessionManager;
import com.cooksync.app.util.constants.UiTimingConstants;
import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

/**
 * Manages a smart recipe-import job's full lifecycle: requesting a Cloudinary upload signature
 * for a photo import, starting the job, polling its status until it reaches a terminal state, and
 * — on success — fetching the resulting (private) recipe so {@link RecipeImportActivity} can hand
 * it straight to the existing edit wizard for review.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class RecipeImportViewModel extends BaseViewModel {

    /**
     * Maps a failed job's stable {@code errorCode} (set server-side, English-only and never
     * meant for display — see {@code RecipeImportServiceImp}) to a localized string, mirroring
     * {@code BaseRepository.ERROR_CODE_MESSAGES}'s HTTP-error-response equivalent. An
     * unrecognized or missing code falls back to {@link R.string#recipe_import_error_generic}.
     */
    private static final Map<String, Integer> JOB_ERROR_MESSAGES = Map.of(
            "ROBOTS_DISALLOWED", R.string.recipe_import_error_robots_disallowed,
            "NO_RECIPE_FOUND_PAGE", R.string.recipe_import_error_no_recipe_page,
            "NO_RECIPE_FOUND_PHOTO", R.string.recipe_import_error_no_recipe_photo,
            "FETCH_FAILED_PAGE", R.string.recipe_import_error_fetch_failed_page,
            "FETCH_FAILED_PHOTO", R.string.recipe_import_error_fetch_failed_photo,
            "IMPORT_FAILED_GENERIC", R.string.recipe_import_error_generic,
            "EXTRACTION_SERVICE_UNAVAILABLE", R.string.recipe_import_error_service_unavailable
    );

    private final RecipeImportRepository recipeImportRepository;
    private final RecipeRepository recipeRepository;
    private final MediaRepository mediaRepository;

    private final MutableLiveData<ApiResult<CloudinarySignatureResponse>> signatureResult = new MutableLiveData<>();
    private final MutableLiveData<RecipeImportJobResponse> jobStatus = new MutableLiveData<>();
    private final MutableLiveData<Event<RecipeResponse>> recipeReadyEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();

    private final PollScheduler pollScheduler;
    /** Resolves a string resource id to text — a real {@code Context::getString} in production,
     *  a fixed stand-in in tests, since {@code CookSyncApplication.getAppContext()} is {@code
     *  null} under this project's plain-JVM unit test runtime (no Robolectric). */
    private final IntFunction<String> stringResolver;
    private Runnable pendingPoll;
    private String pendingFolder;
    private String pendingPublicId;

    /**
     * @param recipeImportRepository the repository used to start/poll import jobs
     * @param recipeRepository the repository used to fetch the resulting recipe once a job succeeds
     * @param mediaRepository the repository used for Cloudinary upload-signature requests (photo import)
     */
    public RecipeImportViewModel(RecipeImportRepository recipeImportRepository, RecipeRepository recipeRepository,
                                  MediaRepository mediaRepository) {
        this(recipeImportRepository, recipeRepository, mediaRepository, new HandlerPollScheduler(),
                resId -> CookSyncApplication.getAppContext().getString(resId));
    }

    /**
     * Test seam: as the public constructor, but with the status-poll scheduler and string
     * resolver injected instead of always creating a real {@link HandlerPollScheduler} (which
     * wraps a genuine Android {@code Handler}) and reading {@code CookSyncApplication}'s real
     * {@code Context} — neither usable from this project's plain-JVM unit tests.
     *
     * @param recipeImportRepository the repository used to start/poll import jobs
     * @param recipeRepository the repository used to fetch the resulting recipe once a job succeeds
     * @param mediaRepository the repository used for Cloudinary upload-signature requests (photo import)
     * @param pollScheduler schedules/cancels the delayed status-poll callback
     * @param stringResolver resolves a string resource id to its text
     */
    RecipeImportViewModel(RecipeImportRepository recipeImportRepository, RecipeRepository recipeRepository,
                           MediaRepository mediaRepository, PollScheduler pollScheduler,
                           IntFunction<String> stringResolver) {
        this.recipeImportRepository = recipeImportRepository;
        this.recipeRepository = recipeRepository;
        this.mediaRepository = mediaRepository;
        this.pollScheduler = pollScheduler;
        this.stringResolver = stringResolver;
    }

    /** @return the outcome of the most recent upload-signature request */
    public LiveData<ApiResult<CloudinarySignatureResponse>> getSignatureResult() { return signatureResult; }
    /** @return the currently in-flight job's latest known status, updated on every poll */
    public LiveData<RecipeImportJobResponse> getJobStatus() { return jobStatus; }
    /** @return a one-shot event firing the imported (private) recipe, ready to open in the edit wizard */
    public LiveData<Event<RecipeResponse>> getRecipeReadyEvent() { return recipeReadyEvent; }
    /** @return a one-shot event firing a human-readable message on any terminal failure */
    public LiveData<Event<String>> getErrorEvent() { return errorEvent; }
    /** @return the folder a picked photo should be uploaded into, set by {@link #requestPhotoUploadSignature} */
    public String getPendingFolder() { return pendingFolder; }
    /** @return the public id a picked photo should be uploaded under, set by {@link #requestPhotoUploadSignature} */
    public String getPendingPublicId() { return pendingPublicId; }

    /**
     * Requests a fresh Cloudinary upload signature for a picked recipe-import photo, mirroring
     * {@code SettingsViewModel#requestUploadSignature}'s folder-then-signature sequence.
     */
    public void requestPhotoUploadSignature() {
        MutableLiveData<ApiResult<String>> baseFolderResult = new MutableLiveData<>();
        observeOnce(baseFolderResult, result -> {
            if (!(result instanceof ApiResult.Success<String> success)) {
                signatureResult.postValue(new ApiResult.Error<>("Failed to resolve upload folder", null));
                return;
            }
            String userEmail = SessionManager.getInstance().getEmail();
            pendingFolder = CloudinaryUploader.buildUserFolder(success.getData(), userEmail, "recipe-import");
            pendingPublicId = "import_" + System.currentTimeMillis();
            mediaRepository.getUploadSignature(pendingFolder, pendingPublicId, signatureResult);
        });
        mediaRepository.getBaseFolder(baseFolderResult);
    }

    /**
     * Starts a web-page import job and begins polling its status.
     *
     * @param sourceUrl the recipe page's URL
     */
    public void startWebImport(String sourceUrl) {
        startImport(new RecipeImportStartRequestDTO("WEB", sourceUrl, null));
    }

    /**
     * Starts a photo import job (every photo must already be uploaded to Cloudinary — see
     * {@link #requestPhotoUploadSignature}, called once per photo) and begins polling its status.
     *
     * @param sourceImageUrls the uploaded photos' secure Cloudinary URLs, in reading order
     */
    public void startPhotoImport(List<String> sourceImageUrls) {
        startImport(new RecipeImportStartRequestDTO("PHOTO", null, sourceImageUrls));
    }

    private void startImport(RecipeImportStartRequestDTO request) {
        MutableLiveData<ApiResult<RecipeImportJobResponse>> startResult = new MutableLiveData<>();
        observeOnce(startResult, result -> {
            if (result instanceof ApiResult.Success<RecipeImportJobResponse> success) {
                jobStatus.setValue(success.getData());
                schedulePoll(success.getData().id());
            } else if (result instanceof ApiResult.Error<RecipeImportJobResponse> error) {
                errorEvent.setValue(new Event<>(error.getMessage()));
            }
        });
        recipeImportRepository.startImport(request, startResult);
    }

    private void schedulePoll(String jobId) {
        cancelPolling();
        pendingPoll = () -> poll(jobId);
        pollScheduler.postDelayed(pendingPoll, UiTimingConstants.RECIPE_IMPORT_POLL_INTERVAL_MS);
    }

    private void poll(String jobId) {
        MutableLiveData<ApiResult<RecipeImportJobResponse>> pollResult = new MutableLiveData<>();
        observeOnce(pollResult, result -> {
            if (result instanceof ApiResult.Success<RecipeImportJobResponse> success) {
                RecipeImportJobResponse job = success.getData();
                jobStatus.setValue(job);
                switch (job.status()) {
                    case "SUCCEEDED" -> fetchReadyRecipe(job.resultRecipeId());
                    case "FAILED" -> errorEvent.setValue(new Event<>(localizedJobError(job.errorCode())));
                    default -> schedulePoll(jobId);
                }
            } else if (result instanceof ApiResult.Error<RecipeImportJobResponse> error) {
                errorEvent.setValue(new Event<>(error.getMessage()));
            }
        });
        recipeImportRepository.getJobStatus(jobId, pollResult);
    }

    /**
     * Resolves a failed job's {@code errorCode} to a localized, user-facing message via
     * {@link #JOB_ERROR_MESSAGES} — never displays the code (or a missing one) verbatim.
     *
     * @param errorCode the job's stable failure code, or {@code null}
     * @return the localized message to show the user
     */
    private String localizedJobError(String errorCode) {
        Integer resId = errorCode == null ? null : JOB_ERROR_MESSAGES.get(errorCode);
        return stringResolver.apply(resId != null ? resId : R.string.recipe_import_error_generic);
    }

    private void fetchReadyRecipe(String recipeId) {
        MutableLiveData<ApiResult<RecipeResponse>> recipeResult = new MutableLiveData<>();
        observeOnce(recipeResult, result -> {
            if (result instanceof ApiResult.Success<RecipeResponse> success) {
                recipeReadyEvent.setValue(new Event<>(success.getData()));
            } else if (result instanceof ApiResult.Error<RecipeResponse> error) {
                errorEvent.setValue(new Event<>(error.getMessage()));
            }
        });
        recipeRepository.getRecipeDetail(recipeId, recipeResult);
    }

    /**
     * Cancels any still-pending poll callback, so it doesn't fire after this screen is gone.
     */
    public void cancelPolling() {
        if (pendingPoll != null) {
            pollScheduler.removeCallbacks(pendingPoll);
            pendingPoll = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelPolling();
    }
}
