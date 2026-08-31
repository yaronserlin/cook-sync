package com.cooksync.app.ui.recipe.cooking;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.instruction.InstructionResponse;
import com.dtos.response.note.NoteResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for the private-note loading, step-navigation, and countdown-timer guard-clause
 * logic on {@link CookingModeViewModel}. The note-loading coverage mirrors the same
 * repository-delegation coverage {@code RecipeDetailViewModelTest} has for
 * {@code RecipeDetailViewModel}.
 *
 * <p>The timer's "actually running" branches ({@code startTimer}'s success path, and
 * {@code toggleTimer}'s pause-while-running / resume-into-a-live-countdown branches) are not
 * covered here: they construct a real {@code android.os.CountDownTimer}, whose constructor and
 * {@code start()} call into {@code Handler}/{@code Looper}/{@code SystemClock} — none of which
 * are mocked by the plain-JVM unit-test {@code android.jar} stub this project uses (no
 * Robolectric, confirmed empirically: both a direct call and a
 * {@code Mockito.mockConstruction(CountDownTimer.class)} attempt fail, the latter because
 * {@code CountDownTimer} is abstract and production code only ever instantiates an anonymous
 * subclass of it inline). Only the reachable no-op guard clauses are covered below.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
public class CookingModeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private RecipeRepository repository;
    private CookingModeViewModel viewModel;

    private final InstructionResponse stepOne = new InstructionResponse(
            "i1", 1, "Sear the beef", false, null, null, null, null, null);
    private final InstructionResponse stepTwo = new InstructionResponse(
            "i2", 2, "Rest, then slice", true, 30, null, null, null, null);
    private final List<InstructionResponse> steps = List.of(stepOne, stepTwo);

    @Before
    public void setUp() {
        repository = mock(RecipeRepository.class);
        viewModel = new CookingModeViewModel(repository);
    }

    @Test
    public void loadNotes_publishesNotesFromRepository() {
        NoteResponse recipeNote = new NoteResponse("n1", "recipe-1", null, "Great recipe");
        NoteResponse stepNote = new NoteResponse("n2", "recipe-1", "step-1", "Whisk gently");
        doAnswer(ApiResultAnswers.success(List.of(recipeNote, stepNote)))
                .when(repository).getAllPersonalNotes(eq("recipe-1"), any());

        viewModel.loadNotes("recipe-1");

        ApiResult<List<NoteResponse>> result = viewModel.getNotesResult().getValue();
        assertTrue(result instanceof ApiResult.Success<List<NoteResponse>>);
        assertEquals(List.of(recipeNote, stepNote), ((ApiResult.Success<List<NoteResponse>>) result).getData());
    }

    @Test
    public void loadNotes_publishesErrorResult_whenRepositoryFails() {
        doAnswer(ApiResultAnswers.<List<NoteResponse>>error("network error"))
                .when(repository).getAllPersonalNotes(eq("recipe-1"), any());

        viewModel.loadNotes("recipe-1");

        ApiResult<List<NoteResponse>> result = viewModel.getNotesResult().getValue();
        assertTrue(result instanceof ApiResult.Error<List<NoteResponse>>);
    }

    @Test
    public void goToStep_negativeIndex_isNoOp() {
        viewModel.goToStep(-1, steps);

        assertEquals(Integer.valueOf(0), viewModel.getCurrentStepIndex().getValue());
    }

    @Test
    public void prevStep_fromFirstStep_isNoOp() {
        viewModel.prevStep(steps);

        assertEquals(Integer.valueOf(0), viewModel.getCurrentStepIndex().getValue());
    }

    @Test
    public void goToStep_indexAtOrPastStepsSize_isNoOp() {
        viewModel.goToStep(steps.size(), steps);

        assertEquals(Integer.valueOf(0), viewModel.getCurrentStepIndex().getValue());
    }

    @Test
    public void nextStep_fromLastStep_isNoOp() {
        viewModel.goToStep(1, steps); // move to the last step first

        viewModel.nextStep(steps);

        assertEquals(Integer.valueOf(1), viewModel.getCurrentStepIndex().getValue());
    }

    @Test
    public void toggleTimer_withNullRemainingSeconds_startTimerIsNoOp() {
        // Fresh ViewModel: timerRemainingSeconds was never set, so it's null.
        viewModel.toggleTimer();

        assertFalse(viewModel.getTimerRunning().getValue());
        assertFalse(viewModel.getTimerStarted().getValue());
    }

    @Test
    public void toggleTimer_withZeroRemainingSeconds_startTimerIsNoOp() {
        InstructionResponse zeroTimerStep = new InstructionResponse(
                "i0", 1, "No time left", true, 0, null, null, null, null);
        viewModel.goToStep(0, List.of(zeroTimerStep)); // sets timerRemainingSeconds to 0

        viewModel.toggleTimer();

        assertFalse(viewModel.getTimerRunning().getValue());
        assertFalse(viewModel.getTimerStarted().getValue());
    }

    @Test
    public void addMinute_withNullRemainingSeconds_setsToOneMinute_withoutStartingTimer() {
        viewModel.addMinute();

        assertEquals(Integer.valueOf(60), viewModel.getTimerRemainingSeconds().getValue());
        assertFalse(viewModel.getTimerRunning().getValue());
    }

    // toggleTimer's pause-while-running and resume-into-a-live-countdown branches are not
    // covered here: reaching timerRunning == true requires startTimer() to successfully
    // construct and start a real android.os.CountDownTimer, whose constructor and start() call
    // into Handler/Looper/SystemClock. None of those are mocked by the plain-JVM unit-test
    // android.jar stub this project uses (no Robolectric) — confirmed empirically, including
    // that Mockito.mockConstruction(CountDownTimer.class) cannot help either, since
    // CountDownTimer is abstract and production code only ever "new"s an anonymous subclass of
    // it inline. See the class Javadoc above.
}
