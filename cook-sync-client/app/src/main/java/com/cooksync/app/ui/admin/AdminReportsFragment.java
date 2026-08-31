package com.cooksync.app.ui.admin;
import com.cooksync.app.ui.base.ViewModelFactory;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.R;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.common.OrganicConfirmDialog;
import com.cooksync.app.ui.common.OrganicToast;
import com.cooksync.app.util.constants.DomainValues;
import com.dtos.response.admin.ReportedReviewResponse;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The Admin Console's Reports tab: reason-filter chips and the queued report cards with
 * Remove/Keep/ban-user actions. Shares {@link AdminReportsViewModel} with the other three tabs
 * via an activity-scoped {@link ViewModelProvider}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public class AdminReportsFragment extends Fragment implements AdminReportAdapter.OnReportActionListener {

    private AdminReportsViewModel viewModel;
    private AdminReportAdapter adapter;
    private TextView tvEmpty;
    private RecyclerView recyclerView;
    private MaterialButton chipAll;
    private MaterialButton chipSpam;
    private MaterialButton chipAbuse;
    private MaterialButton chipOffTopic;

    /**
     * Inflates this tab's layout.
     *
     * @param inflater layout inflater
     * @param container the parent view this fragment's view will attach to, unused
     * @param savedInstanceState previously saved instance state, unused
     * @return the inflated root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_reports, container, false);
    }

    /**
     * Binds the report-card list, wires the reason-filter chips, and subscribes to the
     * ViewModel's results.
     *
     * @param view this fragment's root view
     * @param savedInstanceState previously saved instance state, unused
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new ViewModelFactory()).get(AdminReportsViewModel.class);

        tvEmpty = view.findViewById(R.id.tv_reports_empty);
        recyclerView = view.findViewById(R.id.rv_admin_reports);
        chipAll = view.findViewById(R.id.chip_reason_all);
        chipSpam = view.findViewById(R.id.chip_reason_spam);
        chipAbuse = view.findViewById(R.id.chip_reason_abuse);
        chipOffTopic = view.findViewById(R.id.chip_reason_off_topic);

        adapter = new AdminReportAdapter();
        adapter.setOnReportActionListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(com.cooksync.app.ui.common.PaginatingScrollListener.withThreshold(
                layoutManager, viewModel::loadNextReportsPage));

        chipAll.setOnClickListener(v -> selectReason(AdminReportsViewModel.REASON_ALL));
        chipSpam.setOnClickListener(v -> selectReason(DomainValues.REPORT_REASON_SPAM));
        chipAbuse.setOnClickListener(v -> selectReason(DomainValues.REPORT_REASON_ABUSE));
        chipOffTopic.setOnClickListener(v -> selectReason(DomainValues.REPORT_REASON_OFF_TOPIC));

        observeViewModel();
    }

    /**
     * Applies a reason filter chip selection and restyles the chip row.
     *
     * @param reason the report reason to filter by, or {@link AdminReportsViewModel#REASON_ALL}
     */
    private void selectReason(String reason) {
        viewModel.setReasonFilter(reason);
        styleChips();
    }

    /**
     * Subscribes to the filtered report list and per-row action result.
     */
    private void observeViewModel() {
        viewModel.getFilteredReports().observe(getViewLifecycleOwner(), reports -> {
            List<ReportedReviewResponse> data = reports == null ? Collections.emptyList() : reports;
            adapter.setReports(data);
            recyclerView.setVisibility(data.isEmpty() ? View.GONE : View.VISIBLE);
            tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            updateChipCounts();
        });

        viewModel.getReportActionResult().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;
            ApiResult<Void> result = event.getContentIfNotHandled();
            if (result instanceof ApiResult.Error<Void> error) {
                OrganicToast.showError(requireActivity(), null, error.getMessage());
            }
        });

        styleChips();
    }

    private void updateChipCounts() {
        Map<String, Long> counts = viewModel.getReasonCounts();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        chipAll.setText(getString(R.string.admin_reason_all) + " · " + total);
        chipSpam.setText(getString(R.string.admin_reason_spam) + " · " + counts.getOrDefault(DomainValues.REPORT_REASON_SPAM, 0L));
        chipAbuse.setText(getString(R.string.admin_reason_abuse) + " · " + counts.getOrDefault(DomainValues.REPORT_REASON_ABUSE, 0L));
        chipOffTopic.setText(getString(R.string.admin_reason_off_topic) + " · " + counts.getOrDefault(DomainValues.REPORT_REASON_OFF_TOPIC, 0L));
    }

    private void styleChips() {
        String selected = viewModel.getReasonFilter();
        com.cooksync.app.ui.common.ChipStyler.styleNeutralChip(chipAll, AdminReportsViewModel.REASON_ALL.equals(selected));
        com.cooksync.app.ui.common.ChipStyler.styleNeutralChip(chipSpam, DomainValues.REPORT_REASON_SPAM.equals(selected));
        com.cooksync.app.ui.common.ChipStyler.styleNeutralChip(chipAbuse, DomainValues.REPORT_REASON_ABUSE.equals(selected));
        com.cooksync.app.ui.common.ChipStyler.styleNeutralChip(chipOffTopic, DomainValues.REPORT_REASON_OFF_TOPIC.equals(selected));
    }

    /**
     * Confirms then deletes the reported review (with undo).
     *
     * @param report the report whose review should be deleted
     */
    @Override
    public void onRemove(ReportedReviewResponse report) {
        OrganicConfirmDialog.show(requireContext(),
                getString(R.string.admin_confirm_remove_title),
                getString(R.string.admin_confirm_remove_message),
                getString(R.string.action_remove),
                getString(R.string.action_cancel),
                true,
                () -> {
                    viewModel.removeReport(report);
                    OrganicToast.showWithAction(requireActivity(), null, R.drawable.ic_delete,
                            getString(R.string.admin_toast_report_removed), getString(R.string.action_undo),
                            () -> viewModel.undoRemoveReport(report));
                });
    }

    /**
     * Dismisses the report without deleting its review (with undo).
     *
     * @param report the report to dismiss
     */
    @Override
    public void onKeep(ReportedReviewResponse report) {
        viewModel.keepReport(report);
        OrganicToast.showWithAction(requireActivity(), null, R.drawable.ic_check,
                getString(R.string.admin_toast_report_kept), getString(R.string.action_undo),
                () -> viewModel.undoKeepReport(report));
    }

    /**
     * Confirms then suspends the reported review's author (with undo).
     *
     * @param report the report whose author (reviewer) should be banned
     */
    @Override
    public void onBan(ReportedReviewResponse report) {
        OrganicConfirmDialog.show(requireContext(),
                getString(R.string.admin_confirm_suspend_title),
                getString(R.string.admin_confirm_suspend_message, report.reviewerName()),
                getString(R.string.action_suspend),
                getString(R.string.action_cancel),
                true,
                () -> {
                    viewModel.banReporter(report);
                    OrganicToast.showWithAction(requireActivity(), null, R.drawable.ic_user_x,
                            getString(R.string.admin_toast_user_suspended_format, report.reviewerName()),
                            getString(R.string.action_undo), () -> viewModel.undoBanReporter(report));
                });
    }
}
