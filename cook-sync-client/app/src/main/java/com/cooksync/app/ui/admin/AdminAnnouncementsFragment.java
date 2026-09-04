package com.cooksync.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.BuildConfig;
import com.cooksync.app.R;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.base.ViewModelFactory;
import com.cooksync.app.ui.common.ChipStyler;
import com.cooksync.app.ui.common.OrganicToast;
import com.dtos.response.PagedResponse;
import com.dtos.response.announcement.AnnouncementResponse;
import com.dtos.response.appconfig.AppConfigResponse;

/**
 * Admin Console fragment for composing/broadcasting system announcements and reviewing/
 * deactivating past ones.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class AdminAnnouncementsFragment extends Fragment {

    private AdminAnnouncementsViewModel viewModel;
    private AdminAppConfigViewModel appConfigViewModel;
    private AdminAnnouncementAdapter adapter;
    private EditText etTitle;
    private EditText etBody;
    private TextView chipSeverityInfo;
    private TextView chipSeverityActionRequired;
    private boolean actionRequiredSelected = false;
    private ProgressBar progressBar;
    private EditText etMinVersionCode;
    private EditText etDownloadUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_announcements, container, false);
    }

    /**
     * Binds the announcement list, wires the compose form and row deactivate actions, and
     * triggers the initial announcement-list fetch.
     *
     * @param view this fragment's root view
     * @param savedInstanceState previously saved instance state, unused
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity(), new ViewModelFactory()).get(AdminAnnouncementsViewModel.class);
        appConfigViewModel = new ViewModelProvider(requireActivity(), new ViewModelFactory()).get(AdminAppConfigViewModel.class);

        etMinVersionCode = view.findViewById(R.id.et_min_version_code);
        etDownloadUrl = view.findViewById(R.id.et_download_url);
        ((TextView) view.findViewById(R.id.tv_current_build)).setText(
                getString(R.string.admin_app_config_current_build_format, BuildConfig.VERSION_CODE));

        view.findViewById(R.id.btn_save_app_config).setOnClickListener(v -> {
            String minVersionText = etMinVersionCode.getText().toString().trim();
            if (minVersionText.isEmpty()) {
                OrganicToast.showError(requireActivity(), null, getString(R.string.admin_app_config_validation_required));
                return;
            }
            int minVersionCode;
            try {
                minVersionCode = Integer.parseInt(minVersionText);
            } catch (NumberFormatException e) {
                OrganicToast.showError(requireActivity(), null, getString(R.string.admin_app_config_validation_required));
                return;
            }
            appConfigViewModel.saveConfig(minVersionCode, etDownloadUrl.getText().toString().trim());
        });

        etTitle = view.findViewById(R.id.et_announcement_title);
        etBody = view.findViewById(R.id.et_announcement_body);
        chipSeverityInfo = view.findViewById(R.id.chip_severity_info);
        chipSeverityActionRequired = view.findViewById(R.id.chip_severity_action_required);
        progressBar = view.findViewById(R.id.progress_bar);

        chipSeverityInfo.setOnClickListener(v -> selectSeverityChip(false));
        chipSeverityActionRequired.setOnClickListener(v -> selectSeverityChip(true));
        selectSeverityChip(false);

        RecyclerView rvAnnouncements = view.findViewById(R.id.rv_announcements);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminAnnouncementAdapter();
        rvAnnouncements.setAdapter(adapter);

        adapter.setListener(announcement -> viewModel.deactivateAnnouncement(announcement.id()));

        view.findViewById(R.id.btn_send_announcement).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String body = etBody.getText().toString().trim();
            if (title.isEmpty() || body.isEmpty()) {
                OrganicToast.showError(requireActivity(), null, getString(R.string.admin_announcements_validation_required));
                return;
            }
            String severity = actionRequiredSelected ? "ACTION_REQUIRED" : "INFO";
            viewModel.createAnnouncement(title, body, severity);
        });

        observeViewModel();
        viewModel.loadAnnouncements();
        appConfigViewModel.loadConfig();
    }

    /**
     * Subscribes to the announcement list fetch, announcement-create, and announcement-deactivate
     * results.
     */
    private void observeViewModel() {
        viewModel.getAnnouncementsResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ApiResult.Loading) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (result instanceof ApiResult.Success<PagedResponse<AnnouncementResponse>> success) {
                progressBar.setVisibility(View.GONE);
                adapter.setAnnouncements(success.getData().content());
            } else if (result instanceof ApiResult.Error<?> error) {
                progressBar.setVisibility(View.GONE);
                OrganicToast.showError(requireActivity(), null, error.getMessage());
            }
        });

        viewModel.getCreateResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ApiResult.Success) {
                etTitle.setText("");
                etBody.setText("");
                selectSeverityChip(false);
                OrganicToast.showSuccess(requireActivity(), null, getString(R.string.admin_announcements_create_success));
                viewModel.loadAnnouncements();
            } else if (result instanceof ApiResult.Error<?> error) {
                OrganicToast.showError(requireActivity(), null, error.getMessage());
            }
        });

        viewModel.getDeactivateResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ApiResult.Success) {
                viewModel.loadAnnouncements();
            } else if (result instanceof ApiResult.Error<?> error) {
                OrganicToast.showError(requireActivity(), null, error.getMessage());
            }
        });

        appConfigViewModel.getConfigResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ApiResult.Success<AppConfigResponse> success) {
                AppConfigResponse config = success.getData();
                etMinVersionCode.setText(String.valueOf(config.minSupportedVersionCode()));
                etDownloadUrl.setText(config.downloadUrl());
            } else if (result instanceof ApiResult.Error<?> error) {
                OrganicToast.showError(requireActivity(), null, error.getMessage());
            }
        });

        appConfigViewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof ApiResult.Success) {
                OrganicToast.showSuccess(requireActivity(), null, getString(R.string.admin_app_config_save_success));
            } else if (result instanceof ApiResult.Error<?> error) {
                OrganicToast.showError(requireActivity(), null, error.getMessage());
            }
        });
    }

    /**
     * Selects one of the two severity chips, matching the exclusive-choice chip-row pattern used
     * by the recipe wizard's difficulty/visibility pickers ({@link ChipStyler#styleAccentChip}).
     *
     * @param actionRequired {@code true} to select "Action required", {@code false} for "Info"
     */
    private void selectSeverityChip(boolean actionRequired) {
        actionRequiredSelected = actionRequired;
        ChipStyler.styleAccentChip(chipSeverityInfo, !actionRequired);
        ChipStyler.styleAccentChip(chipSeverityActionRequired, actionRequired);
    }
}
