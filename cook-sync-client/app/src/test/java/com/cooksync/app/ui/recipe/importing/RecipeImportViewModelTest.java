package com.cooksync.app.ui.recipe.importing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.MediaRepository;
import com.cooksync.app.data.repository.RecipeImportRepository;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link RecipeImportViewModel}'s job lifecycle: starting a web/photo import,
 * dispatching the status poll loop to a terminal state, and the upload-signature request's
 * failure branch. A {@link PollScheduler} mock stands in for the real
 * {@link HandlerPollScheduler}, since a genuine {@code Handler} isn't usable from this
 * project's plain-JVM unit test runtime; each captured "postDelayed" runnable is invoked
 * directly to simulate a poll tick firing.
 *
 * <p>{@link RecipeImportViewModel#requestPhotoUploadSignature()}'s success path is not covered
 * here — it reads the cached user email off {@code SessionManager}/{@code TokenStore}, which
 * requires a real Android {@code Context} that {@code TokenStore.init} never receives under a
 * plain JVM test, matching why {@code SettingsViewModel} (which has the same dependency) has no
 * test of its own in this project either.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class RecipeImportViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RecipeImportRepository recipeImportRepository;
    private RecipeRepository recipeRepository;
    private MediaRepository mediaRepository;
    private PollScheduler pollScheduler;
    private RecipeImportViewModel viewModel;

    @Before
    public void setUp() {
        recipeImportRepository = mock(RecipeImportRepository.class);
        recipeRepository = mock(RecipeRepository.class);
        mediaRepository = mock(MediaRepository.class);
        pollScheduler = mock(PollScheduler.class);
        viewModel = new RecipeImportViewModel(recipeImportRepository, recipeRepository, mediaRepository, pollScheduler,
                RecipeImportViewModelTest::fakeStringForResId);
    }

    /** Stand-in for {@code Context.getString(int)}, since the real one needs a real Android
     *  Context that this project's plain-JVM unit tests don't have. */
    private static String fakeStringForResId(int resId) {
        return "STRING_" + resId;
    }

    private RecipeImportJobResponse job(String status, String errorCode, String resultRecipeId) {
        return new RecipeImportJobResponse("job-1", status, errorCode, resultRecipeId, "2026-09-05T10:00:00Z");
    }

    /** Captures the most recently scheduled poll callback, so the test can fire it directly. */
    private Runnable capturePostedPoll() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(pollScheduler, org.mockito.Mockito.atLeastOnce()).postDelayed(captor.capture(), anyLong());
        return captor.getValue();
    }

    @Test
    public void startWebImport_success_publishesJobStatus_andSchedulesPoll() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());

        viewModel.startWebImport("https://example.com/recipe");

        assertEquals("PENDING", viewModel.getJobStatus().getValue().status());
        verify(pollScheduler).postDelayed(any(), eq(com.cooksync.app.util.constants.UiTimingConstants.RECIPE_IMPORT_POLL_INTERVAL_MS));
    }

    @Test
    public void startWebImport_buildsWebRequest_withGivenUrl() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        ArgumentCaptor<RecipeImportStartRequestDTO> captor = ArgumentCaptor.forClass(RecipeImportStartRequestDTO.class);

        viewModel.startWebImport("https://example.com/recipe");

        verify(recipeImportRepository).startImport(captor.capture(), any());
        assertEquals("WEB", captor.getValue().sourceType());
        assertEquals("https://example.com/recipe", captor.getValue().sourceUrl());
        assertNull(captor.getValue().sourceImageUrls());
    }

    @Test
    public void startPhotoImport_buildsPhotoRequest_withGivenImageUrls_inOrder() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        ArgumentCaptor<RecipeImportStartRequestDTO> captor = ArgumentCaptor.forClass(RecipeImportStartRequestDTO.class);
        java.util.List<String> photoUrls = java.util.List.of(
                "https://res.cloudinary.com/photo1.jpg", "https://res.cloudinary.com/photo2.jpg");

        viewModel.startPhotoImport(photoUrls);

        verify(recipeImportRepository).startImport(captor.capture(), any());
        assertEquals("PHOTO", captor.getValue().sourceType());
        assertEquals(photoUrls, captor.getValue().sourceImageUrls());
        assertNull(captor.getValue().sourceUrl());
    }

    @Test
    public void startWebImport_error_firesErrorEvent_andNeverSchedulesPoll() {
        doAnswer(ApiResultAnswers.<RecipeImportJobResponse>error("Rate limit exceeded"))
                .when(recipeImportRepository).startImport(any(), any());

        viewModel.startWebImport("https://example.com/recipe");

        assertEquals("Rate limit exceeded", viewModel.getErrorEvent().getValue().getContentIfNotHandled());
        verify(pollScheduler, never()).postDelayed(any(), anyLong());
    }

    @Test
    public void poll_whenStillProcessing_updatesStatus_andReschedulesPoll() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        viewModel.startWebImport("https://example.com/recipe");
        Runnable firstPoll = capturePostedPoll();

        doAnswer(ApiResultAnswers.success(job("PROCESSING", null, null)))
                .when(recipeImportRepository).getJobStatus(eq("job-1"), any());
        firstPoll.run();

        assertEquals("PROCESSING", viewModel.getJobStatus().getValue().status());
        verify(pollScheduler, times(2)).postDelayed(any(), anyLong());
    }

    @Test
    public void poll_whenSucceeded_fetchesRecipe_andFiresRecipeReadyEvent() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        viewModel.startWebImport("https://example.com/recipe");
        Runnable firstPoll = capturePostedPoll();

        doAnswer(ApiResultAnswers.success(job("SUCCEEDED", null, "recipe-99")))
                .when(recipeImportRepository).getJobStatus(eq("job-1"), any());
        RecipeResponse readyRecipe = new RecipeResponse("recipe-99", null, null, null, null, 0, 0, 0, 0,
                null, null, null, null, null, null, null, null, null, false, null, null);
        doAnswer(ApiResultAnswers.success(readyRecipe))
                .when(recipeRepository).getRecipeDetail(eq("recipe-99"), any());

        firstPoll.run();

        assertEquals("recipe-99", viewModel.getRecipeReadyEvent().getValue().getContentIfNotHandled().id());
    }

    @Test
    public void poll_whenSucceeded_recipeFetchFails_firesErrorEvent() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        viewModel.startWebImport("https://example.com/recipe");
        Runnable firstPoll = capturePostedPoll();

        doAnswer(ApiResultAnswers.success(job("SUCCEEDED", null, "recipe-99")))
                .when(recipeImportRepository).getJobStatus(eq("job-1"), any());
        doAnswer(ApiResultAnswers.<RecipeResponse>error("Recipe not found"))
                .when(recipeRepository).getRecipeDetail(eq("recipe-99"), any());

        firstPoll.run();

        assertEquals("Recipe not found", viewModel.getErrorEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void poll_whenFailed_firesErrorEvent_localizedFromKnownErrorCode() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        viewModel.startWebImport("https://example.com/recipe");
        Runnable firstPoll = capturePostedPoll();

        doAnswer(ApiResultAnswers.success(job("FAILED", "FETCH_FAILED_PAGE", null)))
                .when(recipeImportRepository).getJobStatus(eq("job-1"), any());

        firstPoll.run();

        assertEquals("STRING_" + com.cooksync.app.R.string.recipe_import_error_fetch_failed_page,
                viewModel.getErrorEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void poll_whenFailed_withUnrecognizedErrorCode_firesGenericErrorMessage() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        viewModel.startWebImport("https://example.com/recipe");
        Runnable firstPoll = capturePostedPoll();

        doAnswer(ApiResultAnswers.success(job("FAILED", "SOME_FUTURE_CODE", null)))
                .when(recipeImportRepository).getJobStatus(eq("job-1"), any());

        firstPoll.run();

        assertEquals("STRING_" + com.cooksync.app.R.string.recipe_import_error_generic,
                viewModel.getErrorEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void poll_whenFailed_withNoErrorCode_firesGenericErrorMessage() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        viewModel.startWebImport("https://example.com/recipe");
        Runnable firstPoll = capturePostedPoll();

        doAnswer(ApiResultAnswers.success(job("FAILED", null, null)))
                .when(recipeImportRepository).getJobStatus(eq("job-1"), any());

        firstPoll.run();

        assertEquals("STRING_" + com.cooksync.app.R.string.recipe_import_error_generic,
                viewModel.getErrorEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void poll_networkErrorDuringPoll_firesErrorEvent() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        viewModel.startWebImport("https://example.com/recipe");
        Runnable firstPoll = capturePostedPoll();

        doAnswer(ApiResultAnswers.<RecipeImportJobResponse>error("Connection timed out"))
                .when(recipeImportRepository).getJobStatus(eq("job-1"), any());

        firstPoll.run();

        assertEquals("Connection timed out", viewModel.getErrorEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void cancelPolling_removesTheScheduledCallback() {
        doAnswer(ApiResultAnswers.success(job("PENDING", null, null)))
                .when(recipeImportRepository).startImport(any(), any());
        viewModel.startWebImport("https://example.com/recipe");
        Runnable scheduledPoll = capturePostedPoll();

        viewModel.cancelPolling();

        verify(pollScheduler).removeCallbacks(scheduledPoll);
    }

    @Test
    public void cancelPolling_withNothingPending_doesNotTouchScheduler() {
        viewModel.cancelPolling();

        verify(pollScheduler, never()).removeCallbacks(any());
    }

    @Test
    public void requestPhotoUploadSignature_publishesError_whenBaseFolderLookupFails() {
        doAnswer(ApiResultAnswers.<String>error("No folder configured"))
                .when(mediaRepository).getBaseFolder(any());

        viewModel.requestPhotoUploadSignature();

        ApiResult<CloudinarySignatureResponse> result = viewModel.getSignatureResult().getValue();
        assertTrue(result instanceof ApiResult.Error<CloudinarySignatureResponse>);
        verify(mediaRepository, never()).getUploadSignature(any(), any(), any());
    }
}
