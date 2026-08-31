package com.cooksync.app.ui.recipe.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.note.NoteResponse;
import com.dtos.response.review.ReviewResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unit tests for the pure presentation-logic helpers on {@link RecipeDetailViewModel}:
 * note lookup, star rendering/clamping, and published-date formatting.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
public class RecipeDetailViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RecipeRepository repository;
    private RecipeDetailViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(RecipeRepository.class);
        viewModel = new RecipeDetailViewModel(repository);
    }

    @Test
    public void loadNotes_publishesNotesFromRepository() {
        NoteResponse note = new NoteResponse("n1", "recipe-1", null, "Great recipe");
        doAnswer(ApiResultAnswers.success(List.of(note))).when(repository).getAllPersonalNotes(eq("recipe-1"), any());

        viewModel.loadNotes("recipe-1");

        ApiResult<List<NoteResponse>> result = viewModel.getNotesResult().getValue();
        assertTrue(result instanceof ApiResult.Success<List<NoteResponse>>);
        assertEquals(List.of(note), ((ApiResult.Success<List<NoteResponse>>) result).getData());
    }

    @Test
    public void saveNote_delegatesToRepository_withGivenRecipeInstructionAndText() {
        doAnswer(ApiResultAnswers.<Void>success(null))
                .when(repository).saveNote(eq("recipe-1"), eq("step-1"), eq("Reduce salt"), any());

        viewModel.saveNote("recipe-1", "step-1", "Reduce salt");

        verify(repository).saveNote(eq("recipe-1"), eq("step-1"), eq("Reduce salt"), any());
        ApiResult<Void> result = viewModel.getNoteSaveResult().getValue();
        assertTrue(result instanceof ApiResult.Success<Void>);
    }

    @Test
    public void saveNote_publishesErrorResult_whenRepositoryFails() {
        doAnswer(ApiResultAnswers.<Void>error("network error"))
                .when(repository).saveNote(eq("recipe-1"), isNull(), eq("Great recipe"), any());

        viewModel.saveNote("recipe-1", null, "Great recipe");

        ApiResult<Void> result = viewModel.getNoteSaveResult().getValue();
        assertTrue(result instanceof ApiResult.Error<Void>);
    }

    @Test
    public void deleteNote_delegatesToRepository() {
        doAnswer(ApiResultAnswers.<Void>success(null)).when(repository).deleteNote(eq("note-1"), any());

        viewModel.deleteNote("note-1");

        verify(repository).deleteNote(eq("note-1"), any());
        ApiResult<Void> result = viewModel.getNoteSaveResult().getValue();
        assertTrue(result instanceof ApiResult.Success<Void>);
    }

    @Test
    public void findRecipeNote_returnsTheNoteWithNoInstructionId() {
        NoteResponse recipeNote = new NoteResponse("n1", "recipe-1", null, "Great recipe");
        NoteResponse stepNote = new NoteResponse("n2", "recipe-1", "step-1", "Do this first");

        NoteResponse found = viewModel.findRecipeNote(List.of(stepNote, recipeNote));

        assertEquals(recipeNote, found);
    }

    @Test
    public void findRecipeNote_returnsNull_whenOnlyStepNotesExist() {
        NoteResponse stepNote = new NoteResponse("n2", "recipe-1", "step-1", "Do this first");

        assertNull(viewModel.findRecipeNote(List.of(stepNote)));
    }

    @Test
    public void findStepNote_returnsTheNoteForThatInstruction() {
        NoteResponse step1Note = new NoteResponse("n1", "recipe-1", "step-1", "First");
        NoteResponse step2Note = new NoteResponse("n2", "recipe-1", "step-2", "Second");

        NoteResponse found = viewModel.findStepNote(List.of(step1Note, step2Note), "step-2");

        assertEquals(step2Note, found);
    }

    @Test
    public void findStepNote_returnsNull_whenNoNoteForThatInstruction() {
        NoteResponse step1Note = new NoteResponse("n1", "recipe-1", "step-1", "First");

        assertNull(viewModel.findStepNote(List.of(step1Note), "step-99"));
    }

    @Test
    public void starsForRating_rendersFilledAndOutlineStars() {
        assertEquals("★★★☆☆", viewModel.starsForRating(3.0));
    }

    @Test
    public void starsForRating_roundsToNearestStar() {
        assertEquals("★★★★☆", viewModel.starsForRating(3.6));
    }

    @Test
    public void starsForRating_rendersAllOutline_whenNull() {
        assertEquals("☆☆☆☆☆", viewModel.starsForRating(null));
    }

    @Test
    public void clampStars_roundsToNearestWholeStar() {
        assertEquals(4, viewModel.clampStars(BigDecimal.valueOf(3.6)));
    }

    @Test
    public void clampStars_clampsBelowRangeToOne() {
        assertEquals(1, viewModel.clampStars(BigDecimal.valueOf(0.2)));
    }

    @Test
    public void clampStars_defaultsToOne_whenNull() {
        assertEquals(1, viewModel.clampStars(null));
    }

    @Test
    public void clampStars_clampsAboveRangeToFive() {
        assertEquals(5, viewModel.clampStars(BigDecimal.valueOf(7)));
    }

    @Test
    public void toggleFavorite_addingWhenNotFavorite_callsAddFavoriteImmediately() {
        doAnswer(ApiResultAnswers.<Void>success(null)).when(repository).addFavorite(eq("recipe-1"), any());

        viewModel.toggleFavorite("recipe-1", false);

        verify(repository).addFavorite(eq("recipe-1"), any());
    }

    @Test
    public void toggleFavorite_removingWhenFavorite_defersRemoveFavoriteCall() {
        viewModel.toggleFavorite("recipe-1", true);

        verify(repository, never()).removeFavorite(eq("recipe-1"), any());
    }

    @Test
    public void toggleFavorite_removingWhenFavorite_flushedOnCleared_callsRemoveFavorite() {
        doAnswer(ApiResultAnswers.<Void>success(null)).when(repository).removeFavorite(eq("recipe-1"), any());
        viewModel.toggleFavorite("recipe-1", true);

        viewModel.onCleared();

        verify(repository).removeFavorite(eq("recipe-1"), any());
    }

    @Test
    public void undoRemoveFavorite_beforeWindowElapses_returnsTrue_andNeverCallsRemove() {
        viewModel.toggleFavorite("recipe-1", true);

        assertTrue(viewModel.undoRemoveFavorite("recipe-1"));
        verify(repository, never()).removeFavorite(any(), any());
    }

    @Test
    public void undoRemoveFavorite_returnsFalse_whenNothingPending() {
        assertFalse(viewModel.undoRemoveFavorite("recipe-1"));
    }

    @Test
    public void toggleFavorite_addingWhileMatchingRemoveStillPending_cancelsPendingRemove_withoutCallingAddFavorite() {
        viewModel.toggleFavorite("recipe-1", true); // schedules a deferred remove

        viewModel.toggleFavorite("recipe-1", false); // re-add should cancel it instead

        verify(repository, never()).addFavorite(any(), any());
        verify(repository, never()).removeFavorite(any(), any());
    }

    @Test
    public void getStarBreakdown_countsReviewsByWholeStarRating() {
        List<ReviewResponse> reviews = List.of(
                review("r1", BigDecimal.valueOf(5)),
                review("r2", BigDecimal.valueOf(5)),
                review("r3", BigDecimal.valueOf(3.6)), // rounds to 4
                review("r4", null)); // defaults to 1

        int[] breakdown = viewModel.getStarBreakdown(reviews);

        assertEquals(1, breakdown[1]);
        assertEquals(0, breakdown[2]);
        assertEquals(0, breakdown[3]);
        assertEquals(1, breakdown[4]);
        assertEquals(2, breakdown[5]);
    }

    @Test
    public void getDisplayedReviews_starFilter_removesNonMatchingReviews() {
        ReviewResponse fiveStar = review("r1", BigDecimal.valueOf(5));
        ReviewResponse threeStar = review("r2", BigDecimal.valueOf(3));
        List<ReviewResponse> reviews = List.of(fiveStar, threeStar);

        List<ReviewResponse> displayed = viewModel.getDisplayedReviews(reviews, 5, RecipeDetailViewModel.ReviewSort.NEWEST);

        assertEquals(List.of(fiveStar), displayed);
    }

    @Test
    public void getDisplayedReviews_highestRated_sortsDescending_withNullRatingAsZero() {
        ReviewResponse high = review("r1", BigDecimal.valueOf(4));
        ReviewResponse low = review("r2", BigDecimal.valueOf(2));
        ReviewResponse nullRating = review("r3", null);
        List<ReviewResponse> reviews = List.of(low, nullRating, high);

        List<ReviewResponse> displayed =
                viewModel.getDisplayedReviews(reviews, null, RecipeDetailViewModel.ReviewSort.HIGHEST_RATED);

        assertEquals(List.of(high, low, nullRating), displayed);
    }

    @Test
    public void getDisplayedReviews_lowestRated_sortsAscending_withNullRatingAsZero() {
        ReviewResponse high = review("r1", BigDecimal.valueOf(4));
        ReviewResponse low = review("r2", BigDecimal.valueOf(2));
        ReviewResponse nullRating = review("r3", null);
        List<ReviewResponse> reviews = List.of(high, low, nullRating);

        List<ReviewResponse> displayed =
                viewModel.getDisplayedReviews(reviews, null, RecipeDetailViewModel.ReviewSort.LOWEST_RATED);

        assertEquals(List.of(nullRating, low, high), displayed);
    }

    @Test
    public void getDisplayedReviews_newest_sortsByCreatedAtDescending_withNullCreatedAtAsEmpty() {
        ReviewResponse recent = reviewWithCreatedAt("r1", "2026-03-01");
        ReviewResponse older = reviewWithCreatedAt("r2", "2026-01-01");
        ReviewResponse nullCreatedAt = reviewWithCreatedAt("r3", null);
        List<ReviewResponse> reviews = List.of(older, nullCreatedAt, recent);

        List<ReviewResponse> displayed =
                viewModel.getDisplayedReviews(reviews, null, RecipeDetailViewModel.ReviewSort.NEWEST);

        assertEquals(List.of(recent, older, nullCreatedAt), displayed);
    }

    private ReviewResponse review(String id, BigDecimal rating) {
        return new ReviewResponse(id, "user-1", "Author", null, "recipe-1", rating, "Title", "Comment", "2026-01-01", "2026-01-01");
    }

    private ReviewResponse reviewWithCreatedAt(String id, String createdAt) {
        return new ReviewResponse(id, "user-1", "Author", null, "recipe-1", BigDecimal.valueOf(4), "Title", "Comment", createdAt, createdAt);
    }

    // formatPublishedDate's java.time branch is gated on Build.VERSION.SDK_INT >= O, and the
    // plain JVM unit-test runtime's android.jar stub reports SDK_INT as 0 (no Robolectric),
    // so every case here exercises the pre-Oreo fallback path (a raw 10-character substring)
    // rather than the "Month yyyy" formatting — that branch is only reachable on a real device
    // or under Robolectric.

    @Test
    public void formatPublishedDate_fallsBackToRawDateSubstring_underPlainJvmSdkStub() {
        assertEquals("2026-04-15", viewModel.formatPublishedDate("2026-04-15T10:30:00Z"));
    }

    @Test
    public void formatPublishedDate_returnsEmpty_forNullOrBlank() {
        assertEquals("", viewModel.formatPublishedDate(null));
        assertEquals("", viewModel.formatPublishedDate(""));
    }
}
