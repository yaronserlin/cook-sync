package com.cooksync.app.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cooksync.app.R;
import com.dtos.response.announcement.AnnouncementResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Shared dialog for a system announcement (see the admin-authored broadcast feature). Unlike
 * {@link OrganicConfirmDialog}, this always has exactly one action ("Got it" — dismissing an
 * announcement is not a choice between two outcomes), and an {@code ACTION_REQUIRED} announcement
 * is shown non-cancelable: it cannot be dismissed by tapping outside the dialog or pressing back,
 * only by tapping the button, since the whole point of that severity is that it isn't optional to
 * acknowledge.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public final class AnnouncementDialog {

    private AnnouncementDialog() {
    }

    /**
     * Shows the given announcement as a dialog.
     *
     * @param context the hosting screen's context
     * @param announcement the announcement to display
     * @param onAcknowledge invoked when the user taps "Got it" — the caller is responsible for
     *                      calling the dismiss endpoint so the announcement isn't shown again
     */
    public static void show(@NonNull Context context, @NonNull AnnouncementResponse announcement,
                             @NonNull Runnable onAcknowledge) {
        boolean actionRequired = "ACTION_REQUIRED".equals(announcement.severity());
        new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_CookSync_Dialog)
                .setTitle(announcement.title())
                .setMessage(announcement.body())
                .setCancelable(!actionRequired)
                .setPositiveButton(R.string.announcement_dialog_dismiss, (dialog, which) -> onAcknowledge.run())
                .show();
    }
}
