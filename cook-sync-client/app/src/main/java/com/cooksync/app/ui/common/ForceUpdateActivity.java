package com.cooksync.app.ui.common;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.cooksync.app.R;
import com.cooksync.app.ui.base.BaseActivity;

/**
 * Full-screen, non-dismissable gate shown when {@link com.cooksync.app.data.service.VersionGateManager}
 * determines this build's version code is below the server's configured minimum — the client's
 * "hard" forced-update path, as opposed to the cancelable {@code AnnouncementDialog} shown for a
 * regular (non-blocking) system announcement. Launched by {@link com.cooksync.app.CookSyncApplication}
 * with the back stack cleared, so there is no other screen to navigate back into; the system
 * Back button's only effect is the normal Android behavior of leaving the app (there is no way
 * to prevent that outright, nor any reason to — the point is only that it can't lead anywhere
 * else *inside* the app).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class ForceUpdateActivity extends BaseActivity {

    /** Intent extra carrying the download URL to open, or {@code null} if none is configured yet. */
    public static final String EXTRA_DOWNLOAD_URL = "extra_download_url";

    /**
     * Inflates the screen and wires the download button to the URL passed via
     * {@link #EXTRA_DOWNLOAD_URL}, disabling it if none was configured (the admin hasn't set a
     * download link yet) rather than opening nothing when tapped.
     *
     * @param savedInstanceState previously saved instance state, unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_force_update);

        String downloadUrl = getIntent().getStringExtra(EXTRA_DOWNLOAD_URL);

        View btnDownload = findViewById(R.id.btn_download_update);
        if (downloadUrl == null || downloadUrl.isBlank()) {
            btnDownload.setEnabled(false);
            btnDownload.setAlpha(0.5f);
            return;
        }
        btnDownload.setOnClickListener(v -> openDownloadUrl(downloadUrl));
    }

    /**
     * Opens the download URL in an external browser/app. Failing to find a handler (unlikely —
     * any HTTP(S) URL is handled by the device's browser) surfaces an inline error rather than
     * crashing, since this screen has no other recovery path to fall back to.
     *
     * @param downloadUrl the URL to open
     */
    private void openDownloadUrl(String downloadUrl) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)));
        } catch (ActivityNotFoundException e) {
            showError(getString(R.string.force_update_open_link_failed), findViewById(R.id.btn_download_update));
        }
    }
}
