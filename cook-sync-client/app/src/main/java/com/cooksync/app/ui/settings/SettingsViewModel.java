package com.cooksync.app.ui.settings;

import com.cooksync.app.ui.base.BaseViewModel;
import com.cooksync.app.ui.base.ViewModelFactory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.repository.AuthRepository;
import com.cooksync.app.data.repository.MediaRepository;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.domain.Event;
import com.cooksync.app.util.CloudinaryUploader;
import com.cooksync.app.util.InputSanitizer;
import com.cooksync.app.util.InputValidator;
import com.cooksync.app.util.ResendCooldownTimer;
import com.cooksync.app.util.SessionManager;
import com.dtos.request.auth.AvatarUpdateRequestDTO;
import com.dtos.request.auth.ChangePasswordRequestDTO;
import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.request.auth.VerifyEmailChangeOtpRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.user.UserResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Shared ViewModel backing both {@link SettingsActivity} and {@link AccountDetailsActivity}.
 * Applies client-side validation to every field before submission — mirroring the server's
 * Jakarta constraints, in the same style as {@link com.cooksync.app.ui.auth.LoginViewModel} and
 * {@link com.cooksync.app.ui.auth.RegisterViewModel} — then delegates the actual calls to
 * {@link AuthRepository}. Obtains Cloudinary upload signatures through {@link MediaRepository}
 * for avatar changes, and fetches the Favorites/My recipes counts shown as row subtitles through
 * {@link RecipeRepository}. Sits between the two Settings-area screens and the repository layer,
 * so neither Activity talks to a repository directly.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public class SettingsViewModel extends BaseViewModel {

    private final AuthRepository authRepository;
    private final MediaRepository mediaRepository;
    private final RecipeRepository recipeRepository;

    /** Seconds the email-change OTP dialog's resend button stays disabled after a code is (re)sent. */
    private static final int EMAIL_OTP_RESEND_COOLDOWN_SECONDS = 30;

    private final MutableLiveData<ApiResult<Void>> avatarResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<Void>> requestEmailChangeResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<AuthResponse>> emailOtpResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<Void>> deleteAccountResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<Void>> logoutResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<CloudinarySignatureResponse>> signatureResult = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> validationError = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<List<RecipePreviewResponse>>> favoritesResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<List<RecipePreviewResponse>>> myRecipesResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<UserResponse>> accountDetailsResult = new MutableLiveData<>();
    private final MutableLiveData<Event<ApiResult<Void>>> saveChangesResult = new MutableLiveData<>();
    private final MutableLiveData<Integer> emailOtpResendCooldownSeconds = new MutableLiveData<>(0);
    private final ResendCooldownTimer emailOtpCooldownTimer =
            new ResendCooldownTimer(EMAIL_OTP_RESEND_COOLDOWN_SECONDS, emailOtpResendCooldownSeconds);
    private String pendingFolder;
    private String pendingPublicId;
    /** New email awaiting OTP confirmation, and the password it was requested with; needed to resend. */
    private String pendingNewEmail;
    private String pendingEmailChangePassword;

    /**
     * Constructs the ViewModel with its collaborating repositories, injected by
     * {@link com.cooksync.app.ui.base.ViewModelFactory}.
     *
     * @param authRepository the repository used for profile/password/email/account calls
     * @param mediaRepository the repository used for Cloudinary upload-signature requests
     * @param recipeRepository the repository used to fetch the Favorites/My recipes counts
     */
    public SettingsViewModel(AuthRepository authRepository, MediaRepository mediaRepository,
                              RecipeRepository recipeRepository) {
        this.authRepository = authRepository;
        this.mediaRepository = mediaRepository;
        this.recipeRepository = recipeRepository;
    }

    /**
     * Fetches the current user's full profile — including city, bio, and privacy preferences —
     * used to pre-fill the Account Details screen.
     */
    public void loadAccountDetails() {
        authRepository.getCurrentUserProfile(accountDetailsResult);
    }

    /**
     * Requests a fresh Cloudinary upload signature, used immediately before uploading a newly
     * picked avatar image. First resolves the server-configured root upload folder, then builds
     * the target folder as {@code [baseFolder]/[userEmail]/avatar} before requesting the
     * signature itself.
     */
    public void requestUploadSignature() {
        MutableLiveData<ApiResult<String>> baseFolderResult = new MutableLiveData<>();
        observeOnce(baseFolderResult, result -> {
            if (!(result instanceof ApiResult.Success<String> success)) {
                signatureResult.postValue(new ApiResult.Error<>(
                        "Failed to resolve upload folder", null));
                return;
            }

            String userId = SessionManager.getInstance().getUserId();
            String userEmail = SessionManager.getInstance().getEmail();
            String first = SessionManager.getInstance().getFirstName() == null ? "" : SessionManager.getInstance().getFirstName().trim();
            String last = SessionManager.getInstance().getLastName() == null ? "" : SessionManager.getInstance().getLastName().trim();
            pendingFolder = CloudinaryUploader.buildUserFolder(success.getData(), userEmail, "avatar");
            pendingPublicId = first + "_" + last + "_" + userId + "_" + System.currentTimeMillis();
            mediaRepository.getUploadSignature(pendingFolder, pendingPublicId, signatureResult);
        });
        mediaRepository.getBaseFolder(baseFolderResult);
    }

    /**
     * Persists a newly uploaded avatar's URL against the user's account. Called after the image
     * itself has already been uploaded to Cloudinary directly by the view layer.
     *
     * @param avatarUrl the secure URL Cloudinary returned for the uploaded image
     */
    public void updateAvatar(String avatarUrl) {
        authRepository.updateAvatar(new AvatarUpdateRequestDTO(avatarUrl), avatarResult);
    }

    /**
     * Validates and submits an email-change request, re-authenticated against the current
     * password. On success the server has emailed a verification code to the new address; the
     * address and password are stashed so {@link #resendEmailChangeOtp()} can re-submit the same
     * request, and the resend cooldown starts immediately since a fresh code was just sent.
     *
     * @param rawNewEmail        raw text from the new-email field
     * @param rawCurrentPassword raw text from the current-password field
     */
    public void requestEmailChange(String rawNewEmail, String rawCurrentPassword) {
        InputValidator.ValidationResult emailRes = InputValidator.validateEmail(rawNewEmail);
        if (!emailRes.isValid) {
            validationError.setValue(new Event<>(emailRes.errorMessage));
            return;
        }
        InputValidator.ValidationResult passwordRes = InputValidator.validateLoginPassword(rawCurrentPassword);
        if (!passwordRes.isValid) {
            validationError.setValue(new Event<>(passwordRes.errorMessage));
            return;
        }
        pendingNewEmail = rawNewEmail.trim();
        pendingEmailChangePassword = rawCurrentPassword;
        observeOnce(requestEmailChangeResult, result -> {
            if (result instanceof ApiResult.Success) {
                emailOtpCooldownTimer.start();
            }
        });
        authRepository.requestEmailChange(new EmailUpdateRequestDTO(pendingNewEmail, pendingEmailChangePassword), requestEmailChangeResult);
    }

    /**
     * Re-submits the same email-change request as {@link #requestEmailChange}, using the
     * address/password stashed from the original call, restarting the resend cooldown on
     * success. Backs the email-change OTP dialog's "Resend code" action. No-op while the
     * cooldown from a previous send is still running.
     */
    public void resendEmailChangeOtp() {
        resendWithCooldown(emailOtpResendCooldownSeconds, emailOtpCooldownTimer, requestEmailChangeResult,
                () -> authRepository.requestEmailChange(new EmailUpdateRequestDTO(pendingNewEmail, pendingEmailChangePassword), requestEmailChangeResult));
    }

    /**
     * Validates and submits the OTP code verifying the pending email change. On success, clears
     * the stashed pending address/password, since the change is now complete.
     *
     * @param rawCode raw text from the OTP code field
     */
    public void verifyEmailChangeOtp(String rawCode) {
        String code = InputSanitizer.trim(rawCode);
        InputValidator.ValidationResult codeRes = InputValidator.validateOtpCode(code);
        if (!codeRes.isValid) {
            validationError.setValue(new Event<>(codeRes.errorMessage));
            return;
        }
        String newEmail = pendingNewEmail;
        observeOnce(emailOtpResult, result -> {
            if (result instanceof ApiResult.Success) {
                pendingNewEmail = null;
                pendingEmailChangePassword = null;
                emailOtpCooldownTimer.cancel();
            }
        });
        authRepository.verifyEmailChangeOtp(new VerifyEmailChangeOtpRequestDTO(code), newEmail, emailOtpResult);
    }

    /**
     * Submits the profile (name/city/bio), privacy settings, and — if a new password was
     * entered — a password change, as a single batch. Every applicable request is fired in
     * parallel, and the batch reports exactly one combined outcome via
     * {@link #getSaveChangesResult()} once every fired request has settled, so the caller can
     * navigate away on success without racing any individual call. A client-side validation
     * failure (invalid name, weak password, mismatched repeat) fails fast and fires no network
     * call at all.
     *
     * @param rawFirstName          raw text from the first-name field
     * @param rawLastName           raw text from the last-name field
     * @param rawCity               raw text from the city field, may be blank
     * @param rawBio                raw text from the bio field, may be blank
     * @param showRecipesPublicly   whether published recipes appear on the public profile
     * @param showFavoritesPublicly whether favorited recipes are visible to other users
     * @param rawCurrentPassword    raw text from the current-password field, required only if
     *                              {@code rawNewPassword} is non-blank
     * @param rawNewPassword        raw text from the new-password field, or blank to leave the
     *                              password unchanged
     * @param rawRepeatPassword     raw text from the repeat-new-password field
     */
    public void saveAccountChanges(String rawFirstName, String rawLastName, String rawCity, String rawBio,
                                    boolean showRecipesPublicly, boolean showFavoritesPublicly,
                                    String rawCurrentPassword, String rawNewPassword, String rawRepeatPassword) {
        InputValidator.ValidationResult firstRes = InputValidator.validateName(rawFirstName, "First name");
        if (!firstRes.isValid) {
            validationError.setValue(new Event<>(firstRes.errorMessage));
            return;
        }
        InputValidator.ValidationResult lastRes = InputValidator.validateName(rawLastName, "Last name");
        if (!lastRes.isValid) {
            validationError.setValue(new Event<>(lastRes.errorMessage));
            return;
        }

        boolean changingPassword = rawNewPassword != null && !rawNewPassword.isEmpty();
        if (changingPassword) {
            InputValidator.ValidationResult currentRes = InputValidator.validateLoginPassword(rawCurrentPassword);
            if (!currentRes.isValid) {
                validationError.setValue(new Event<>(currentRes.errorMessage));
                return;
            }
            InputValidator.ValidationResult newRes = InputValidator.validateNewPassword(rawNewPassword);
            if (!newRes.isValid) {
                validationError.setValue(new Event<>(newRes.errorMessage));
                return;
            }
            InputValidator.ValidationResult matchRes = InputValidator.validatePasswordsMatch(rawNewPassword, rawRepeatPassword);
            if (!matchRes.isValid) {
                validationError.setValue(new Event<>(matchRes.errorMessage));
                return;
            }
        }

        int totalCalls = changingPassword ? 3 : 2;
        AtomicInteger remaining = new AtomicInteger(totalCalls);
        AtomicReference<String> firstError = new AtomicReference<>();
        Consumer<ApiResult<Void>> onEachSettled = result -> {
            if (result instanceof ApiResult.Error<Void> error) {
                firstError.compareAndSet(null, error.getMessage());
            }
            if (remaining.decrementAndGet() == 0) {
                String error = firstError.get();
                saveChangesResult.postValue(new Event<>(error != null
                        ? new ApiResult.Error<>(error, null)
                        : new ApiResult.Success<>(null)));
            }
        };

        String city = rawCity == null || rawCity.trim().isEmpty() ? null : rawCity.trim();
        String bio = rawBio == null || rawBio.trim().isEmpty() ? null : rawBio.trim();
        MutableLiveData<ApiResult<Void>> profileTarget = new MutableLiveData<>();
        observeOnce(profileTarget, onEachSettled);
        authRepository.updateProfile(new ProfileUpdateRequestDTO(rawFirstName.trim(), rawLastName.trim(), city, bio), profileTarget);

        MutableLiveData<ApiResult<Void>> privacyTarget = new MutableLiveData<>();
        observeOnce(privacyTarget, onEachSettled);
        authRepository.updatePrivacySettings(new PrivacySettingsUpdateRequestDTO(showRecipesPublicly, showFavoritesPublicly), privacyTarget);

        if (changingPassword) {
            MutableLiveData<ApiResult<Void>> passwordTarget = new MutableLiveData<>();
            observeOnce(passwordTarget, onEachSettled);
            authRepository.changePassword(new ChangePasswordRequestDTO(rawCurrentPassword, rawNewPassword), passwordTarget);
        }
    }

    /**
     * Validates and submits an account-deletion request, starting the server's 30-day grace
     * period.
     *
     * @param rawCurrentPassword raw text from the current-password confirmation field
     */
    public void deleteAccount(String rawCurrentPassword) {
        InputValidator.ValidationResult passwordRes = InputValidator.validateLoginPassword(rawCurrentPassword);
        if (!passwordRes.isValid) {
            validationError.setValue(new Event<>(passwordRes.errorMessage));
            return;
        }
        authRepository.requestAccountDeletion(new DeleteAccountRequestDTO(rawCurrentPassword), deleteAccountResult);
    }

    /**
     * Logs the current user out of the app.
     */
    public void logout() {
        authRepository.logout(logoutResult);
    }

    /**
     * Fetches the current user's favorite recipes, from which the "Favorites" row's subtitle
     * count is derived.
     */
    public void loadFavoritesCount() {
        recipeRepository.getFavorites(favoritesResult);
    }

    /**
     * Fetches the current user's own recipes, from which the "My recipes" row's subtitle count
     * is derived.
     */
    public void loadMyRecipesCount() {
        recipeRepository.getMyRecipes(myRecipesResult);
    }

    /** @return observable result of an avatar URL update */
    public LiveData<ApiResult<Void>> getAvatarResult() { return avatarResult; }
    /** @return observable result of an email-change request (the OTP-send step) */
    public LiveData<ApiResult<Void>> getRequestEmailChangeResult() { return requestEmailChangeResult; }
    /** @return observable result of email-change OTP verification (the apply step) */
    public LiveData<ApiResult<AuthResponse>> getEmailOtpResult() { return emailOtpResult; }
    /** @return observable seconds remaining before the email-change OTP can be resent, 0 when allowed */
    public LiveData<Integer> getEmailOtpResendCooldownSeconds() { return emailOtpResendCooldownSeconds; }
    /** @return observable result of an account-deletion request */
    public LiveData<ApiResult<Void>> getDeleteAccountResult() { return deleteAccountResult; }
    /** @return one-shot combined outcome of {@link #saveAccountChanges}, success only once every fired call has succeeded */
    public LiveData<Event<ApiResult<Void>>> getSaveChangesResult() { return saveChangesResult; }
    /** @return observable result of a logout request */
    public LiveData<ApiResult<Void>> getLogoutResult() { return logoutResult; }
    /** @return observable result of a Cloudinary upload-signature request */
    public LiveData<ApiResult<CloudinarySignatureResponse>> getSignatureResult() { return signatureResult; }
    /** @return one-shot client-side validation errors, surfaced by the view as a Toast */
    public LiveData<Event<String>> getValidationError() { return validationError; }
    /** @return observable result of the Favorites list fetch, used to derive its row's count */
    public LiveData<ApiResult<List<RecipePreviewResponse>>> getFavoritesResult() { return favoritesResult; }
    /** @return observable result of the My recipes list fetch, used to derive its row's count */
    public LiveData<ApiResult<List<RecipePreviewResponse>>> getMyRecipesResult() { return myRecipesResult; }
    /** @return observable result of the current user's full profile fetch */
    public LiveData<ApiResult<UserResponse>> getAccountDetailsResult() { return accountDetailsResult; }
    /** @return the target Cloudinary folder resolved for the in-flight avatar upload */
    public String getPendingFolder() { return pendingFolder; }
    /** @return the target Cloudinary public ID resolved for the in-flight avatar upload */
    public String getPendingPublicId() { return pendingPublicId; }

    /**
     * Compares the account details form's current field values against their last-known-saved
     * baseline, deciding whether leaving the screen should prompt a "discard changes?"
     * confirmation. The Activity supplies the current values since it owns the widgets; this
     * method owns only the "what counts as changed" rule.
     *
     * @param firstName current value of the first-name field
     * @param lastName current value of the last-name field
     * @param city current value of the city field
     * @param bio current value of the bio field
     * @param email current value of the email field
     * @param currentPassword current value of the current-password field
     * @param newPassword current value of the new-password field
     * @param repeatNewPassword current value of the repeat-new-password field
     * @param showRecipesPublicly current state of the "show recipes publicly" checkbox
     * @param showFavoritesPublicly current state of the "show favorites publicly" checkbox
     * @param pendingAvatarChange whether a new photo was picked or the avatar was cleared, but
     *                            not yet saved
     * @param baselineFirstName first name as last loaded/saved
     * @param baselineLastName last name as last loaded/saved
     * @param baselineCity city as last loaded/saved
     * @param baselineBio bio as last loaded/saved
     * @param baselineEmail email as last loaded/saved
     * @param baselineShowRecipesPublicly "show recipes publicly" as last loaded/saved
     * @param baselineShowFavoritesPublicly "show favorites publicly" as last loaded/saved
     * @return {@code true} if any field or the avatar differs from what's actually saved
     */
    public boolean hasUnsavedAccountChanges(
            String firstName, String lastName, String city, String bio, String email,
            String currentPassword, String newPassword, String repeatNewPassword,
            boolean showRecipesPublicly, boolean showFavoritesPublicly, boolean pendingAvatarChange,
            String baselineFirstName, String baselineLastName, String baselineCity, String baselineBio,
            String baselineEmail, boolean baselineShowRecipesPublicly, boolean baselineShowFavoritesPublicly) {
        if (pendingAvatarChange) return true;
        if (!firstName.trim().equals(baselineFirstName)) return true;
        if (!lastName.trim().equals(baselineLastName)) return true;
        if (!city.trim().equals(baselineCity)) return true;
        if (!bio.trim().equals(baselineBio)) return true;
        if (!email.trim().equalsIgnoreCase(baselineEmail)) return true;
        if (!currentPassword.isEmpty()) return true;
        if (!newPassword.isEmpty()) return true;
        if (!repeatNewPassword.isEmpty()) return true;
        if (showRecipesPublicly != baselineShowRecipesPublicly) return true;
        if (showFavoritesPublicly != baselineShowFavoritesPublicly) return true;
        return false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        emailOtpCooldownTimer.cancel();
    }
}
