package com.cooksync.app.ui.settings;

import com.cooksync.app.ui.base.BaseActivity;
import com.cooksync.app.ui.base.Navigator;
import com.cooksync.app.ui.base.ViewModelFactory;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cooksync.app.R;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.common.OrganicConfirmDialog;
import com.cooksync.app.ui.common.OrganicToast;
import com.cooksync.app.util.CloudinaryUploader;
import com.cooksync.app.util.GlideUtils;
import com.cooksync.app.util.LocalImageCache;
import com.cooksync.app.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import com.dtos.response.user.UserResponse;

import java.util.Objects;

/**
 * Dedicated screen for managing every self-service account setting in one place: name, city,
 * bio, email, password, avatar, public-profile privacy toggles, and account deletion. Reached
 * from the "Account details" row on {@link SettingsActivity}, corresponding to the design's
 * {@code is.edit} screen.
 *
 * <p>Binds to the same {@link SettingsViewModel} instance type that {@link SettingsActivity}
 * uses, rather than introducing a separate business-logic layer — this Activity is simply another
 * View over the same ViewModel and repository calls. Each edited section is submitted only when
 * its own "Save changes" tap fires the network calls for the fields that actually changed,
 * mirroring the granularity of the server's {@code AuthController} endpoints.</p>
 *
 * <p>A picked avatar (or "Use initials instead") is held only as a local preview until "Save
 * changes" is tapped — nothing is uploaded or persisted before then, matching every other field
 * on this screen. Leaving the screen (back arrow, system back, or Cancel) while any field is
 * unsaved — including a pending avatar change — prompts a discard-confirmation dialog first.</p>
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 08/08/2026
 */
public class AccountDetailsActivity extends BaseActivity {

    private SettingsViewModel viewModel;

    private ImageView ivAvatar;
    private TextView tvAvatarInitials;
    private ProgressBar avatarProgress;
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etEmail;
    private EditText etCity;
    private EditText etBio;
    private EditText etCurrentPassword;
    private EditText etNewPassword;
    private EditText etRepeatNewPassword;
    private MaterialCheckBox cbShowRecipesPublicly;
    private MaterialCheckBox cbShowFavoritesPublicly;
    private View footer;

    /** Prefix applied to this screen's cached avatar picks, distinguishing them within the shared app-wide cache. */
    private static final String AVATAR_CACHE_PREFIX = "avatar_pick_";

    private ActivityResultLauncher<String> pickAvatarLauncher;

    /** A newly picked photo not yet uploaded or saved; mutually exclusive with {@link #avatarCleared}. */
    private Uri pendingAvatarUri;
    /** Whether "Use initials instead" was tapped but the change has not yet been saved. */
    private boolean avatarCleared;

    /** The email-change OTP dialog while it's on screen, {@code null} otherwise. */
    private AlertDialog emailOtpDialog;
    /** The new address the currently-open OTP dialog is verifying, for the success toast/baseline update. */
    private String pendingOtpNewEmail;

    // Baseline values loaded from the server, used both to detect what changed on Save and to
    // detect unsaved edits on exit. Updated after each section's own successful save.
    private String loadedFirstName = "";
    private String loadedLastName = "";
    private String loadedEmail = "";
    private String loadedCity = "";
    private String loadedBio = "";
    private boolean loadedShowRecipesPublicly = true;
    private boolean loadedShowFavoritesPublicly = false;

    /**
     * Wires up the avatar picker launcher, the exit-confirmation back-press callback, view
     * bindings, observers, and every button's click listener, then triggers the initial
     * {@link SettingsViewModel#loadAccountDetails()} fetch.
     *
     * @param savedInstanceState previously saved instance state, unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        viewModel = new ViewModelProvider(this, new ViewModelFactory()).get(SettingsViewModel.class);

        pickAvatarLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                LocalImageCache.copyToPrivateCache(this, uri, AVATAR_CACHE_PREFIX, localUri -> {
                    if (localUri == null) {
                        return;
                    }
                    pendingAvatarUri = localUri;
                    avatarCleared = false;
                    tvAvatarInitials.setVisibility(View.GONE);
                    Glide.with(this).load(localUri).transform(new CircleCrop()).into(ivAvatar);
                    OrganicToast.show(this, footer, getString(R.string.account_details_avatar_pending));
                });
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptExit();
            }
        });

        bindViews();
        renderCachedProfile();
        setupObservers();

        findViewById(R.id.btn_back).setOnClickListener(v -> attemptExit());
        findViewById(R.id.btn_edit_avatar).setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        findViewById(R.id.btn_upload_photo).setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        findViewById(R.id.btn_use_initials).setOnClickListener(v -> {
            pendingAvatarUri = null;
            avatarCleared = true;
            renderAvatar(null);
            OrganicToast.show(this, footer, getString(R.string.account_details_avatar_cleared_pending));
        });
        findViewById(R.id.btn_cancel).setOnClickListener(v -> attemptExit());
        findViewById(R.id.btn_save).setOnClickListener(v -> onSaveClicked());
        findViewById(R.id.btn_delete_account).setOnClickListener(v -> confirmDeleteAccount());

        viewModel.loadAccountDetails();
    }

    /**
     * Binds every view reference from the inflated layout.
     */
    private void bindViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        tvAvatarInitials = findViewById(R.id.tv_avatar_initials);
        avatarProgress = findViewById(R.id.avatar_progress);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etCity = findViewById(R.id.et_city);
        etBio = findViewById(R.id.et_bio);
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etRepeatNewPassword = findViewById(R.id.et_repeat_new_password);
        cbShowRecipesPublicly = findViewById(R.id.cb_show_recipes_publicly);
        cbShowFavoritesPublicly = findViewById(R.id.cb_show_favorites_publicly);
        footer = findViewById(R.id.footer);
    }

    /**
     * Pre-fills the form with whatever is already cached locally, so the screen is not blank
     * while {@link SettingsViewModel#loadAccountDetails()}'s network call is in flight. City,
     * bio, and privacy preferences are not part of the local cache and are filled in only once
     * that call resolves.
     */
    private void renderCachedProfile() {
        loadedFirstName = Objects.requireNonNullElse(SessionManager.getInstance().getFirstName(), "");
        loadedLastName = Objects.requireNonNullElse(SessionManager.getInstance().getLastName(), "");
        loadedEmail = Objects.requireNonNullElse(SessionManager.getInstance().getEmail(), "");
        etFirstName.setText(loadedFirstName);
        etLastName.setText(loadedLastName);
        etEmail.setText(loadedEmail);
        renderAvatar(SessionManager.getInstance().getAvatarUrl());
    }

    /**
     * Renders either the given avatar photo or the fallback initials badge into this screen's
     * avatar views.
     *
     * @param avatarUrl the avatar photo URL to render, or {@code null}/blank to show initials
     */
    private void renderAvatar(String avatarUrl) {
        GlideUtils.renderAvatarOrInitials(Glide.with(this), avatarUrl, ivAvatar, tvAvatarInitials,
                SessionManager.getInstance().getInitials());
    }

    /**
     * Toggles the avatar progress spinner and disables the avatar-editing buttons while an
     * upload is in flight.
     *
     * @param uploading {@code true} while an avatar upload is in progress
     */
    private void setAvatarUploading(boolean uploading) {
        avatarProgress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        ((MaterialButton) findViewById(R.id.btn_upload_photo)).setEnabled(!uploading);
        findViewById(R.id.btn_edit_avatar).setEnabled(!uploading);
    }

    /**
     * Subscribes to every {@link SettingsViewModel} LiveData stream this screen reacts to:
     * validation errors, the account-details fetch, the avatar upload/signature/update chain,
     * and the email-change OTP dialog's request/resend/verify flow.
     */
    private void setupObservers() {
        viewModel.getValidationError().observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) showError(message, footer);
        });

        viewModel.getAccountDetailsResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<UserResponse> success) {
                UserResponse data = success.getData();
                loadedFirstName = Objects.requireNonNullElse(data.firstName(), "");
                loadedLastName = Objects.requireNonNullElse(data.lastName(), "");
                loadedEmail = Objects.requireNonNullElse(data.email(), "");
                loadedCity = Objects.requireNonNullElse(data.city(), "");
                loadedBio = Objects.requireNonNullElse(data.bio(), "");
                loadedShowRecipesPublicly = data.showRecipesPublicly();
                loadedShowFavoritesPublicly = data.showFavoritesPublicly();
                etFirstName.setText(loadedFirstName);
                etLastName.setText(loadedLastName);
                etEmail.setText(loadedEmail);
                etCity.setText(loadedCity);
                etBio.setText(loadedBio);
                cbShowRecipesPublicly.setChecked(loadedShowRecipesPublicly);
                cbShowFavoritesPublicly.setChecked(loadedShowFavoritesPublicly);
                if (pendingAvatarUri == null && !avatarCleared) {
                    renderAvatar(data.avatarUrl());
                }
            } else if (result instanceof ApiResult.Error<?> error) {
                showError(error.getMessage(), footer);
            }
        });

        viewModel.getSignatureResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<CloudinarySignatureResponse> success && pendingAvatarUri != null) {
                CloudinaryUploader.upload(this, pendingAvatarUri, viewModel.getPendingFolder(), viewModel.getPendingPublicId(), success.getData(), new CloudinaryUploader.Callback() {
                    @Override
                    public void onSuccess(@NonNull String secureUrl) {
                        viewModel.updateAvatar(secureUrl);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        setAvatarUploading(false);
                        showError(message, footer);
                    }
                });
            } else if (result instanceof ApiResult.Error<?> error) {
                setAvatarUploading(false);
                showError(error.getMessage(), footer);
            }
        });

        viewModel.getAvatarResult().observe(this, result -> {
            if (result instanceof ApiResult.Success) {
                setAvatarUploading(false);
                pendingAvatarUri = null;
                avatarCleared = false;
                renderAvatar(SessionManager.getInstance().getAvatarUrl());
                LocalImageCache.clearCache(this, AVATAR_CACHE_PREFIX);
                saveRemainingProfileChanges();
            } else if (result instanceof ApiResult.Error<?> error) {
                setAvatarUploading(false);
                renderAvatar(SessionManager.getInstance().getAvatarUrl());
                showError(error.getMessage(), footer);
            }
        });

        viewModel.getRequestEmailChangeResult().observe(this, result -> {
            // Fires both for the initial request (dialog not yet open) and for a resend from
            // the OTP dialog's own button (dialog already open) — routed accordingly below so a
            // resend never stacks a second dialog on top of the first.
            if (result instanceof ApiResult.Success) {
                if (emailOtpDialog == null) {
                    showEmailOtpDialog(etEmail.getText().toString().trim());
                }
            } else if (result instanceof ApiResult.Error<?> error) {
                if (emailOtpDialog != null) {
                    showEmailOtpError(error.getMessage());
                } else {
                    showError(error.getMessage(), footer);
                }
            }
        });

        viewModel.getEmailOtpResendCooldownSeconds().observe(this, seconds -> {
            if (emailOtpDialog == null) return;
            MaterialButton btnResend = emailOtpDialog.findViewById(R.id.btn_resend);
            if (btnResend == null) return;
            if (seconds == null || seconds <= 0) {
                btnResend.setEnabled(true);
                btnResend.setText(R.string.action_resend_code);
            } else {
                btnResend.setEnabled(false);
                btnResend.setText(getString(R.string.action_resend_code_countdown, seconds));
            }
        });

        viewModel.getEmailOtpResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<AuthResponse>) {
                if (emailOtpDialog != null) {
                    emailOtpDialog.dismiss();
                }
                loadedEmail = pendingOtpNewEmail;
                showSuccess(getString(R.string.settings_email_updated), footer);
                submitAccountChanges();
            } else if (result instanceof ApiResult.Error<?> error) {
                showEmailOtpError(error.getMessage());
            }
        });

        viewModel.getSaveChangesResult().observe(this, event -> {
            ApiResult<Void> result = event.getContentIfNotHandled();
            if (result == null) return;
            if (result instanceof ApiResult.Success) {
                loadedFirstName = etFirstName.getText().toString().trim();
                loadedLastName = etLastName.getText().toString().trim();
                loadedCity = etCity.getText().toString().trim();
                loadedBio = etBio.getText().toString().trim();
                loadedShowRecipesPublicly = cbShowRecipesPublicly.isChecked();
                loadedShowFavoritesPublicly = cbShowFavoritesPublicly.isChecked();
                etCurrentPassword.setText("");
                etNewPassword.setText("");
                etRepeatNewPassword.setText("");

                Intent extras = new Intent();
                extras.putExtra(SettingsActivity.EXTRA_PENDING_TOAST, getString(R.string.settings_updated));
                Navigator.start(AccountDetailsActivity.this, SettingsActivity.class, extras);
                finish();
            } else if (result instanceof ApiResult.Error<Void> error) {
                showError(error.getMessage(), footer);
            }
        });

        viewModel.getDeleteAccountResult().observe(this, result -> {
            if (result instanceof ApiResult.Success) {
                showSuccess(getString(R.string.account_details_deletion_requested), footer);
            } else if (result instanceof ApiResult.Error<?> error) {
                showError(error.getMessage(), footer);
            }
        });
    }

    /**
     * Kicks off saving the form. A pending avatar change (new photo or "use initials") is
     * resolved first: {@link #saveRemainingProfileChanges()} runs only once that single call has
     * actually settled, from {@link SettingsViewModel#getAvatarResult()}'s success handler, so it
     * never fires twice for the same tap and never races the avatar write into
     * {@link SessionManager}. With no pending avatar change, the rest of the form is submitted
     * right away.
     */
    private void onSaveClicked() {
        if (pendingAvatarUri != null) {
            setAvatarUploading(true);
            viewModel.requestUploadSignature();
        } else if (avatarCleared) {
            setAvatarUploading(true);
            viewModel.updateAvatar(null);
        } else {
            saveRemainingProfileChanges();
        }
    }

    /**
     * If the email field changed, gates the entire save behind a password-confirmation dialog
     * followed by an OTP-verification dialog: {@link #submitAccountChanges()} runs only once the
     * new address has actually been confirmed, from the {@code emailOtpResult} success observer
     * in {@link #setupObservers()}, so it never fires while either dialog is still on screen.
     * Cancelling either dialog abandons the whole save attempt — the email-change code the
     * server already issued is simply left to expire, same as an abandoned forgot-password code.
     * With no email change, the batch is submitted right away.
     */
    private void saveRemainingProfileChanges() {
        String newEmail = etEmail.getText().toString().trim();
        if (!newEmail.equalsIgnoreCase(loadedEmail)) {
            showEmailChangeDialog(newEmail);
        } else {
            submitAccountChanges();
        }
    }

    /**
     * Prompts for the current password before requesting {@code newEmail}, since changing email
     * requires re-authentication and the "current password" field on this screen is dedicated to
     * the password-change section rather than implicitly reused for email too. On confirm, asks
     * the server to verify the password and email a one-time code to {@code newEmail}; the OTP
     * dialog itself opens from the {@code requestEmailChangeResult} success observer once that
     * call actually succeeds.
     *
     * @param newEmail the new email address to submit once the password is confirmed
     */
    private void showEmailChangeDialog(String newEmail) {
        OrganicConfirmDialog.showWithPasswordConfirm(this,
                getString(R.string.account_details_dialog_change_email_title),
                getString(R.string.account_details_dialog_change_email_message, newEmail),
                getString(R.string.account_details_action_change_email),
                getString(R.string.action_cancel),
                password -> viewModel.requestEmailChange(newEmail, password));
    }

    /**
     * Opens the OTP-verification dialog for a pending email change, once the server has
     * confirmed a code was actually sent to {@code newEmail}. Tapping "Verify" submits the
     * entered code; tapping "Resend" re-requests a code without closing the dialog; cancelling
     * (or dismissing by any other means) abandons the whole save attempt, matching the password
     * dialog's cancel behavior. {@link #emailOtpDialog} is always cleared when the dialog closes
     * — via the dismiss listener below — so a later email-change attempt can open a fresh one
     * instead of finding the field still pointing at a closed dialog.
     *
     * @param newEmail the pending new address the code was sent to, shown in the dialog message
     */
    private void showEmailOtpDialog(String newEmail) {
        pendingOtpNewEmail = newEmail;
        emailOtpDialog = OrganicConfirmDialog.showWithOtpConfirm(this,
                getString(R.string.account_details_dialog_email_otp_title),
                getString(R.string.account_details_dialog_email_otp_message, newEmail),
                getString(R.string.action_verify),
                getString(R.string.action_cancel),
                viewModel::verifyEmailChangeOtp,
                viewModel::resendEmailChangeOtp);
        emailOtpDialog.setOnDismissListener(d -> emailOtpDialog = null);
    }

    /**
     * Surfaces an email-change OTP error inline in the still-open OTP dialog, so the user can
     * correct the code without losing their place (e.g. having to re-enter their password).
     *
     * @param message the server's error message (invalid code, expired code, or too many attempts)
     */
    private void showEmailOtpError(String message) {
        if (emailOtpDialog == null) return;
        TextView tvOtpError = emailOtpDialog.findViewById(R.id.tv_otp_error);
        if (tvOtpError == null) return;
        tvOtpError.setText(message);
        tvOtpError.setVisibility(View.VISIBLE);
    }

    /**
     * Submits the name/city/bio/privacy/password batch (see
     * {@link SettingsViewModel#saveAccountChanges}). On success this navigates to
     * {@link SettingsActivity}, so it must run only once any required email confirmation has
     * already been resolved.
     */
    private void submitAccountChanges() {
        viewModel.saveAccountChanges(
                etFirstName.getText().toString(),
                etLastName.getText().toString(),
                etCity.getText().toString(),
                etBio.getText().toString(),
                cbShowRecipesPublicly.isChecked(),
                cbShowFavoritesPublicly.isChecked(),
                etCurrentPassword.getText().toString(),
                etNewPassword.getText().toString(),
                etRepeatNewPassword.getText().toString());
    }

    /**
     * Shows the account-deletion confirmation dialog, which collects the current password before
     * starting the 30-day self-service deletion grace period via {@link SettingsViewModel#deleteAccount}.
     */
    private void confirmDeleteAccount() {
        OrganicConfirmDialog.showWithPasswordConfirm(this,
                getString(R.string.account_details_dialog_delete_title),
                getString(R.string.account_details_dialog_delete_message),
                getString(R.string.account_details_action_delete),
                getString(R.string.action_cancel),
                viewModel::deleteAccount);
    }

    /**
     * Leaves the screen immediately if nothing is unsaved; otherwise asks the user to confirm
     * discarding their edits first.
     */
    private void attemptExit() {
        if (!hasUnsavedChanges()) {
            finish();
            return;
        }
        OrganicConfirmDialog.show(this,
                getString(R.string.account_details_discard_title),
                getString(R.string.account_details_discard_message),
                getString(R.string.account_details_discard_confirm),
                getString(R.string.account_details_discard_keep_editing),
                true,
                this::finish);
    }

    /**
     * Compares every editable field, plus any pending, not-yet-saved avatar change, against its
     * last-known-saved baseline.
     *
     * @return {@code true} if any field or the avatar differs from what is actually saved
     */
    private boolean hasUnsavedChanges() {
        return viewModel.hasUnsavedAccountChanges(
                etFirstName.getText().toString(), etLastName.getText().toString(),
                etCity.getText().toString(), etBio.getText().toString(), etEmail.getText().toString(),
                etCurrentPassword.getText().toString(), etNewPassword.getText().toString(),
                etRepeatNewPassword.getText().toString(),
                cbShowRecipesPublicly.isChecked(), cbShowFavoritesPublicly.isChecked(),
                pendingAvatarUri != null || avatarCleared,
                loadedFirstName, loadedLastName, loadedCity, loadedBio, loadedEmail,
                loadedShowRecipesPublicly, loadedShowFavoritesPublicly);
    }
}
