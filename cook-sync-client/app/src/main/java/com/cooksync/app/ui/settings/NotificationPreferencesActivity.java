package com.cooksync.app.ui.settings;

import com.cooksync.app.ui.base.BaseActivity;
import com.cooksync.app.ui.base.ViewModelFactory;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;

import com.cooksync.app.R;
import com.cooksync.app.domain.ApiResult;
import com.dtos.response.notification.NotificationPreferencesResponse;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Screen for editing the authenticated user's notification preferences: whether push
 * notifications are enabled at all, and whether system announcements are among them. Unlike
 * {@link CookingPreferencesActivity} (device-local, no network), these settings are server-synced
 * so they follow the user across devices — reads and writes go through
 * {@link NotificationPreferencesViewModel} rather than directly to local storage.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class NotificationPreferencesActivity extends BaseActivity {

    private NotificationPreferencesViewModel viewModel;
    private SwitchMaterial switchPushEnabled;
    private SwitchMaterial switchSystemAnnouncements;

    /** Guards against the initial programmatic switch state triggering a spurious save call. */
    private boolean suppressSaveOnChange = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_preferences);

        viewModel = new ViewModelProvider(this, new ViewModelFactory()).get(NotificationPreferencesViewModel.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        switchPushEnabled = findViewById(R.id.switch_push_enabled);
        switchSystemAnnouncements = findViewById(R.id.switch_system_announcements);
        switchPushEnabled.setEnabled(false);
        switchSystemAnnouncements.setEnabled(false);

        setupObservers();
        viewModel.loadPreferences();
    }

    /**
     * Subscribes to the initial preferences fetch (populating and enabling both switches without
     * triggering a save) and to save-result failures (surfaced as an error toast).
     */
    private void setupObservers() {
        viewModel.getPreferencesResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<NotificationPreferencesResponse> success) {
                NotificationPreferencesResponse preferences = success.getData();
                suppressSaveOnChange = true;
                switchPushEnabled.setChecked(preferences.pushEnabled());
                switchSystemAnnouncements.setChecked(preferences.systemAnnouncements());
                suppressSaveOnChange = false;
                switchPushEnabled.setEnabled(true);
                switchSystemAnnouncements.setEnabled(true);
                wireToggleListeners();
            } else if (result instanceof ApiResult.Error<NotificationPreferencesResponse> error) {
                showError(error.getMessage(), switchPushEnabled);
            }
        });

        viewModel.getUpdateResult().observe(this, result -> {
            if (result instanceof ApiResult.Error<Void> error) {
                showError(error.getMessage(), switchPushEnabled);
            }
        });
    }

    /**
     * Attaches each switch's save-on-toggle listener, only after the initial fetch has populated
     * both switches — attaching earlier would risk saving stale/default values.
     */
    private void wireToggleListeners() {
        switchPushEnabled.setOnCheckedChangeListener((button, checked) -> {
            if (suppressSaveOnChange) return;
            viewModel.setPushEnabled(switchSystemAnnouncements.isChecked(), checked);
        });
        switchSystemAnnouncements.setOnCheckedChangeListener((button, checked) -> {
            if (suppressSaveOnChange) return;
            viewModel.setSystemAnnouncements(checked, switchPushEnabled.isChecked());
        });
    }
}
