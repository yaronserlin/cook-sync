package com.cooksync.app.ui.auth;

import com.cooksync.app.ui.base.BaseViewModel;
import com.cooksync.app.ui.base.ViewModelFactory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.repository.AuthRepository;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.util.InputSanitizer;
import com.cooksync.app.util.InputValidator;
import com.cooksync.app.util.ResendCooldownTimer;
import com.dtos.request.auth.ForgotPasswordRequestDTO;
import com.dtos.request.auth.ResetPasswordRequestDTO;

/**
 * ViewModel for {@link ForgotPasswordActivity}. Drives both stages of the forgot-password
 * flow: requesting a reset code by email, then consuming that code to set a new password. A
 * client-side {@link ResendCooldownTimer}, shared with {@link VerifyOtpViewModel}, disables the
 * resend action for {@value #RESEND_COOLDOWN_SECONDS} seconds after a code is issued or resent.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/08/2026
 */
public class ForgotPasswordViewModel extends BaseViewModel {

    /** Seconds the resend button stays disabled after a code is issued or resent. */
    private static final int RESEND_COOLDOWN_SECONDS = 30;

    private final AuthRepository authRepository;

    private String email;

    private final MutableLiveData<ApiResult<Void>> forgotPasswordResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<Void>> resendCodeResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<Void>> resetPasswordResult = new MutableLiveData<>();
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> codeError = new MutableLiveData<>();
    private final MutableLiveData<String> newPasswordError = new MutableLiveData<>();
    private final MutableLiveData<String> repeatPasswordError = new MutableLiveData<>();
    private final MutableLiveData<Integer> resendCooldownSeconds = new MutableLiveData<>(0);

    private final ResendCooldownTimer cooldownTimer = new ResendCooldownTimer(RESEND_COOLDOWN_SECONDS, resendCooldownSeconds);

    /**
     * Constructs the ViewModel with the given {@link AuthRepository}, injected by
     * {@link com.cooksync.app.ui.base.ViewModelFactory}.
     *
     * @param authRepository the repository used for the forgot/reset password calls
     */
    public ForgotPasswordViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * Validates and submits the forgot-password email request. On success, starts the resend
     * cooldown for the reset stage that's about to be revealed.
     *
     * @param rawEmail raw text from the email {@code EditText}
     */
    public void requestReset(String rawEmail) {
        String trimmedEmail = InputSanitizer.trim(rawEmail);
        InputValidator.ValidationResult emResult = InputValidator.validateEmail(trimmedEmail);
        emailError.setValue(emResult.errorMessage);
        if (!emResult.isValid) {
            return;
        }
        this.email = trimmedEmail;
        observeOnce(forgotPasswordResult, result -> {
            if (result instanceof ApiResult.Success) {
                cooldownTimer.start();
            }
        });
        authRepository.forgotPassword(new ForgotPasswordRequestDTO(trimmedEmail), forgotPasswordResult);
    }

    /**
     * Requests a fresh reset code for the email captured by {@link #requestReset}, restarting
     * the resend cooldown on success. No-op while the cooldown from a previous send is still
     * running.
     */
    public void resendCode() {
        resendWithCooldown(resendCooldownSeconds, cooldownTimer, resendCodeResult,
                () -> authRepository.forgotPassword(new ForgotPasswordRequestDTO(email), resendCodeResult));
    }

    /**
     * Validates and submits the new password using the emailed reset code.
     *
     * @param rawCode            raw text from the reset-code {@code EditText}
     * @param rawNewPassword     raw text from the new-password {@code EditText}
     * @param rawRepeatPassword  raw text from the repeat-password {@code EditText}
     */
    public void resetPassword(String rawCode, String rawNewPassword, String rawRepeatPassword) {
        String code = InputSanitizer.trim(rawCode);
        String newPassword = rawNewPassword == null ? "" : rawNewPassword;
        String repeatPassword = rawRepeatPassword == null ? "" : rawRepeatPassword;

        InputValidator.ValidationResult codeResult = InputValidator.validateOtpCode(code);
        InputValidator.ValidationResult pwResult = InputValidator.validateNewPassword(newPassword);
        InputValidator.ValidationResult rpResult = InputValidator.validatePasswordsMatch(newPassword, repeatPassword);

        codeError.setValue(codeResult.errorMessage);
        newPasswordError.setValue(pwResult.errorMessage);
        repeatPasswordError.setValue(rpResult.errorMessage);

        if (!codeResult.isValid || !pwResult.isValid || !rpResult.isValid) {
            return;
        }

        authRepository.resetPassword(new ResetPasswordRequestDTO(email, code, newPassword), resetPasswordResult);
    }

    /** @return observable forgot-password request result (Loading → Success/Error) */
    public LiveData<ApiResult<Void>> getForgotPasswordResult() { return forgotPasswordResult; }
    /** @return observable resend-code result (Loading → Success/Error) */
    public LiveData<ApiResult<Void>> getResendCodeResult() { return resendCodeResult; }
    /** @return observable reset-password result (Loading → Success/Error) */
    public LiveData<ApiResult<Void>> getResetPasswordResult() { return resetPasswordResult; }
    /** @return observable email error, {@code null} when valid */
    public LiveData<String> getEmailError() { return emailError; }
    /** @return observable reset-code error, {@code null} when valid */
    public LiveData<String> getCodeError() { return codeError; }
    /** @return observable new-password policy error, {@code null} when valid */
    public LiveData<String> getNewPasswordError() { return newPasswordError; }
    /** @return observable repeat-password mismatch error, {@code null} when valid */
    public LiveData<String> getRepeatPasswordError() { return repeatPasswordError; }
    /** @return observable seconds remaining before resend is allowed again, 0 when allowed */
    public LiveData<Integer> getResendCooldownSeconds() { return resendCooldownSeconds; }

    @Override
    protected void onCleared() {
        super.onCleared();
        cooldownTimer.cancel();
    }
}
