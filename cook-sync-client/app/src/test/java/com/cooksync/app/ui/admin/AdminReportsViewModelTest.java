package com.cooksync.app.ui.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.cooksync.app.data.repository.AdminRepository;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.domain.Event;
import com.cooksync.app.testutil.ApiResultAnswers;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.ReportedReviewResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unit tests for {@link AdminReportsViewModel}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
public class AdminReportsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AdminRepository adminRepository;
    private RecipeRepository recipeRepository;
    private AdminReportsViewModel viewModel;

    private final ReportedReviewResponse spamReport = new ReportedReviewResponse(
            "r1", "Bob", "user-bob", null, "recipe-1", "Chili", "SPAM",
            "Buy my stuff", null, BigDecimal.valueOf(1), "2026-08-01T00:00:00Z");
    private final ReportedReviewResponse abuseReportSameUser = new ReportedReviewResponse(
            "r2", "Bob", "user-bob", null, "recipe-2", "Soup", "ABUSE",
            "Rude comment", null, BigDecimal.valueOf(1), "2026-08-02T00:00:00Z");
    private final ReportedReviewResponse otherUserReport = new ReportedReviewResponse(
            "r3", "Carol", "user-carol", null, "recipe-3", "Stew", "OFF_TOPIC",
            "Unrelated", null, BigDecimal.valueOf(3), "2026-08-03T00:00:00Z");

    @Before
    public void setUp() {
        adminRepository = mock(AdminRepository.class);
        recipeRepository = mock(RecipeRepository.class);
        viewModel = new AdminReportsViewModel(adminRepository, recipeRepository);
    }

    @Test
    public void loadReportedReviews_publishesFirstPage() {
        loadOnePageWithAllThreeReports();

        List<ReportedReviewResponse> reports = viewModel.getFilteredReports().getValue();
        assertEquals(List.of(spamReport, abuseReportSameUser, otherUserReport), reports);
    }

    @Test
    public void loadReportedReviews_error_postsErrorEvent() {
        doAnswer(ApiResultAnswers.<PagedResponse<ReportedReviewResponse>>error("network error"))
                .when(adminRepository).getReportedReviews(eq(0), eq(20), any());

        viewModel.loadReportedReviews();

        Event<ApiResult<Void>> event = viewModel.getReportActionResult().getValue();
        ApiResult<Void> result = event.getContentIfNotHandled();
        assertTrue(result instanceof ApiResult.Error<Void>);
        assertEquals("network error", ((ApiResult.Error<Void>) result).getMessage());
    }

    @Test
    public void setReasonFilter_filtersClientSideWithoutNetworkCall() {
        loadOnePageWithAllThreeReports();

        viewModel.setReasonFilter("SPAM");

        assertEquals(List.of(spamReport), viewModel.getFilteredReports().getValue());
        verify(adminRepository, never()).getReportedReviews(eq(1), anyInt(), any());
    }

    @Test
    public void removeReport_hidesImmediately_undoneBeforeFlush_restoresAndNeverDeletes() {
        loadOnePageWithAllThreeReports();

        viewModel.removeReport(spamReport);
        assertEquals(List.of(abuseReportSameUser, otherUserReport), viewModel.getFilteredReports().getValue());

        viewModel.undoRemoveReport(spamReport);
        assertEquals(List.of(abuseReportSameUser, otherUserReport, spamReport), viewModel.getFilteredReports().getValue());
        verify(recipeRepository, never()).deleteReview(any(), any());
    }

    @Test
    public void removeReport_flushedWithoutUndo_deletesTheUnderlyingReview() {
        loadOnePageWithAllThreeReports();

        viewModel.removeReport(spamReport);
        viewModel.onCleared();

        verify(recipeRepository).deleteReview(eq("r1"), any());
    }

    @Test
    public void removeReport_flushedWithoutUndo_serverError_restoresReport_andPostsErrorEvent() {
        loadOnePageWithAllThreeReports();
        doAnswer(ApiResultAnswers.<Void>error("network error"))
                .when(recipeRepository).deleteReview(eq("r1"), any());

        viewModel.removeReport(spamReport);
        viewModel.onCleared();

        assertEquals(List.of(abuseReportSameUser, otherUserReport, spamReport), viewModel.getFilteredReports().getValue());
        ApiResult<Void> result = viewModel.getReportActionResult().getValue().getContentIfNotHandled();
        assertTrue(result instanceof ApiResult.Error<Void>);
        assertEquals("network error", ((ApiResult.Error<Void>) result).getMessage());
    }

    @Test
    public void keepReport_hidesImmediately_undoneBeforeFlush_restoresAndNeverDismisses() {
        loadOnePageWithAllThreeReports();

        viewModel.keepReport(spamReport);
        assertEquals(List.of(abuseReportSameUser, otherUserReport), viewModel.getFilteredReports().getValue());

        viewModel.undoKeepReport(spamReport);
        assertEquals(List.of(abuseReportSameUser, otherUserReport, spamReport), viewModel.getFilteredReports().getValue());
        verify(adminRepository, never()).dismissReport(any(), any());
    }

    @Test
    public void keepReport_flushedWithoutUndo_dismissesTheReportWithoutDeletingTheReview() {
        loadOnePageWithAllThreeReports();
        doAnswer(ApiResultAnswers.<Void>success(null))
                .when(adminRepository).dismissReport(eq("r1"), any());

        viewModel.keepReport(spamReport);
        viewModel.onCleared();

        verify(adminRepository).dismissReport(eq("r1"), any());
        verify(recipeRepository, never()).deleteReview(any(), any());
        assertEquals(List.of(abuseReportSameUser, otherUserReport), viewModel.getFilteredReports().getValue());
    }

    @Test
    public void removeReportsForUser_removesEveryReportFromThatReviewer() {
        loadOnePageWithAllThreeReports();

        viewModel.removeReportsForUser("user-bob");

        assertEquals(List.of(otherUserReport), viewModel.getFilteredReports().getValue());
    }

    @Test
    public void banReporter_removesReporterQueuedReports_andDisablesThemOnFlush() {
        loadOnePageWithAllThreeReports();
        doAnswer(ApiResultAnswers.<Void>success(null))
                .when(adminRepository).suspendUser(eq("user-bob"), any());

        viewModel.banReporter(spamReport);
        assertEquals(List.of(otherUserReport), viewModel.getFilteredReports().getValue());

        viewModel.onCleared();
        verify(adminRepository).suspendUser(eq("user-bob"), any());
    }

    @Test
    public void banReporter_serverError_reloadsQueue_andPostsErrorEvent() {
        loadOnePageWithAllThreeReports();
        doAnswer(ApiResultAnswers.<Void>error("network error"))
                .when(adminRepository).suspendUser(eq("user-bob"), any());

        viewModel.banReporter(spamReport);
        assertEquals(List.of(otherUserReport), viewModel.getFilteredReports().getValue());

        viewModel.onCleared();

        assertEquals(List.of(spamReport, abuseReportSameUser, otherUserReport), viewModel.getFilteredReports().getValue());
        ApiResult<Void> result = viewModel.getReportActionResult().getValue().getContentIfNotHandled();
        assertTrue(result instanceof ApiResult.Error<Void>);
        assertEquals("network error", ((ApiResult.Error<Void>) result).getMessage());
    }

    private void loadOnePageWithAllThreeReports() {
        PagedResponse<ReportedReviewResponse> page = new PagedResponse<>(
                List.of(spamReport, abuseReportSameUser, otherUserReport), 0, 20, 3, 1, true);
        doAnswer(ApiResultAnswers.success(page))
                .when(adminRepository).getReportedReviews(eq(0), eq(20), any());
        viewModel.loadReportedReviews();
    }
}
